package com.example.kafkaadmin.service;

import com.example.kafkaadmin.config.AdminKafkaProperties;
import com.example.kafkaadmin.dto.ActualizarConfigRequest;
import com.example.kafkaadmin.dto.ConsumerGroupResponse;
import com.example.kafkaadmin.dto.CrearTopicoRequest;
import com.example.kafkaadmin.dto.LagResponse;
import com.example.kafkaadmin.dto.ParticionInfo;
import com.example.kafkaadmin.dto.ResetOffsetsRequest;
import com.example.kafkaadmin.dto.TopicoResponse;
import com.example.kafkaadmin.exception.ConflictoException;
import com.example.kafkaadmin.exception.OperacionKafkaException;
import com.example.kafkaadmin.exception.RecursoNoEncontradoException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Capa de gestion de Kafka sobre el AdminClient oficial: topicos (crear, listar, describir, modificar
 * configuracion, aumentar particiones, eliminar) y consumer groups (listar, describir, lag, reset).
 *
 * Cada operacion tiene un timeout y los errores de Kafka se traducen a excepciones de la API
 * (404, 409, 400, 503) en vez de propagar ExecutionException al cliente.
 */
@Service
public class KafkaAdminService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    /** Configuraciones que se muestran al describir un topico. */
    private static final List<String> CONFIGS_VISIBLES = List.of(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.RETENTION_MS_CONFIG,
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
            TopicConfig.SEGMENT_MS_CONFIG, TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG);

    private final AdminClient admin;
    private final AdminKafkaProperties props;

    public KafkaAdminService(AdminClient admin, AdminKafkaProperties props) {
        this.admin = admin;
        this.props = props;
    }

    // ------------------------------------------------------------------ topicos

    public List<TopicoResponse> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names(), "listar topicos");
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames(), "describir topicos");
        return descripciones.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(d -> aRespuesta(d, Map.of()))
                .toList();
    }

    public TopicoResponse obtenerTopico(String nombre) {
        TopicDescription d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames(), "describir " + nombre).get(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all(), "leer configuracion de " + nombre).get(recurso);
        Map<String, String> visibles = new LinkedHashMap<>();
        if (config != null) {
            for (String clave : CONFIGS_VISIBLES) {
                ConfigEntry entrada = config.get(clave);
                if (entrada != null) {
                    visibles.put(clave, entrada.value());
                }
            }
        }
        return aRespuesta(d, visibles);
    }

    public TopicoResponse crearTopico(CrearTopicoRequest req, String usuario) {
        if (req.particiones() > props.maxParticiones()) {
            throw new IllegalArgumentException("Maximo " + props.maxParticiones() + " particiones por topico");
        }
        if (req.minInsyncReplicas() != null && req.minInsyncReplicas() > req.replicas()) {
            throw new IllegalArgumentException("minInsyncReplicas no puede ser mayor que replicas");
        }
        Map<String, String> configs = new HashMap<>();
        if (req.retencionMs() != null) {
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(req.retencionMs()));
        }
        if (req.cleanupPolicy() != null) {
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, req.cleanupPolicy());
        }
        if (req.minInsyncReplicas() != null) {
            configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(req.minInsyncReplicas()));
        }
        NewTopic topico = new NewTopic(req.nombre(), req.particiones(), req.replicas()).configs(configs);
        esperar(admin.createTopics(List.of(topico)).all(), "crear topico " + req.nombre());
        auditoria.info("accion=CREAR_TOPICO topico={} particiones={} replicas={} configs={} usuario={}",
                req.nombre(), req.particiones(), req.replicas(), configs, usuario);
        return obtenerTopico(req.nombre());
    }

    public TopicoResponse actualizarConfiguracion(String nombre, ActualizarConfigRequest req, String usuario) {
        if (req.vacio()) {
            throw new IllegalArgumentException("Debe indicar al menos una configuracion a modificar");
        }
        List<AlterConfigOp> cambios = new ArrayList<>();
        agregar(cambios, TopicConfig.RETENTION_MS_CONFIG, req.retencionMs());
        agregar(cambios, TopicConfig.CLEANUP_POLICY_CONFIG, req.cleanupPolicy());
        agregar(cambios, TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, req.minInsyncReplicas());
        agregar(cambios, TopicConfig.MAX_MESSAGE_BYTES_CONFIG, req.maxMessageBytes());
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Map<ConfigResource, Collection<AlterConfigOp>> operacion = Map.of(recurso, cambios);
        esperar(admin.incrementalAlterConfigs(operacion).all(), "modificar configuracion de " + nombre);
        auditoria.info("accion=MODIFICAR_CONFIG topico={} cambios={} usuario={}", nombre, req, usuario);
        return obtenerTopico(nombre);
    }

    public TopicoResponse aumentarParticiones(String nombre, int total, String usuario) {
        if (total > props.maxParticiones()) {
            throw new IllegalArgumentException("Maximo " + props.maxParticiones() + " particiones por topico");
        }
        int actuales = obtenerTopico(nombre).particiones();
        if (total <= actuales) {
            throw new IllegalArgumentException("Kafka solo permite aumentar particiones: el topico ya tiene " + actuales);
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all(), "aumentar particiones de " + nombre);
        auditoria.info("accion=AUMENTAR_PARTICIONES topico={} de={} a={} usuario={}", nombre, actuales, total, usuario);
        return obtenerTopico(nombre);
    }

    public void eliminarTopico(String nombre, String usuario) {
        if (props.esProtegido(nombre)) {
            throw new ConflictoException("El topico '" + nombre + "' es parte del sistema y no se puede eliminar");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all(), "eliminar topico " + nombre);
        auditoria.info("accion=ELIMINAR_TOPICO topico={} usuario={}", nombre, usuario);
    }

    // ------------------------------------------------------------- consumer groups

    public List<ConsumerGroupResponse> listarConsumerGroups() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().all(), "listar consumer groups");
        if (grupos.isEmpty()) {
            return List.of();
        }
        List<String> ids = grupos.stream().map(ConsumerGroupListing::groupId).sorted().toList();
        Map<String, ConsumerGroupDescription> desc = esperar(admin.describeConsumerGroups(ids).all(), "describir consumer groups");
        return ids.stream().map(id -> aRespuesta(desc.get(id))).toList();
    }

    public ConsumerGroupResponse obtenerConsumerGroup(String grupo) {
        Map<String, ConsumerGroupDescription> desc =
                esperar(admin.describeConsumerGroups(List.of(grupo)).all(), "describir consumer group " + grupo);
        return aRespuesta(desc.get(grupo));
    }

    /** Lag por particion = log-end-offset - offset confirmado por el grupo. */
    public LagResponse calcularLag(String grupo) {
        Map<TopicPartition, OffsetAndMetadata> confirmados = esperar(
                admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata(), "leer offsets de " + grupo);
        if (confirmados.isEmpty()) {
            throw new RecursoNoEncontradoException("El consumer group '" + grupo + "' no tiene offsets confirmados");
        }
        Map<TopicPartition, OffsetSpec> pedir = new HashMap<>();
        confirmados.keySet().forEach(tp -> pedir.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                esperar(admin.listOffsets(pedir).all(), "leer offsets finales");

        List<LagResponse.LagParticion> detalle = new ArrayList<>();
        long total = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : confirmados.entrySet()) {
            long consumido = e.getValue() != null ? e.getValue().offset() : 0L;
            ListOffsetsResult.ListOffsetsResultInfo fin = finales.get(e.getKey());
            long finalOffset = fin != null ? fin.offset() : consumido;
            long lag = Math.max(0, finalOffset - consumido);
            total += lag;
            detalle.add(new LagResponse.LagParticion(e.getKey().topic(), e.getKey().partition(), consumido, finalOffset, lag));
        }
        detalle.sort(Comparator.comparing(LagResponse.LagParticion::topico).thenComparing(LagResponse.LagParticion::particion));
        return new LagResponse(grupo, total, detalle);
    }

    /**
     * Reposiciona el grupo al inicio (re-procesar todo) o al final (saltar lo pendiente) de un topico.
     * Kafka exige que el grupo no tenga consumidores activos: si los tiene responde 409.
     */
    public LagResponse resetearOffsets(String grupo, ResetOffsetsRequest req, String usuario) {
        TopicoResponse topico = obtenerTopico(req.topico());
        OffsetSpec spec = "earliest".equals(req.destino()) ? OffsetSpec.earliest() : OffsetSpec.latest();
        Map<TopicPartition, OffsetSpec> pedir = new HashMap<>();
        for (int p = 0; p < topico.particiones(); p++) {
            pedir.put(new TopicPartition(req.topico(), p), spec);
        }
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets =
                esperar(admin.listOffsets(pedir).all(), "leer offsets de " + req.topico());
        Map<TopicPartition, OffsetAndMetadata> nuevos = new HashMap<>();
        offsets.forEach((tp, info) -> nuevos.put(tp, new OffsetAndMetadata(info.offset())));
        esperar(admin.alterConsumerGroupOffsets(grupo, nuevos).all(), "resetear offsets de " + grupo);
        auditoria.info("accion=RESET_OFFSETS grupo={} topico={} destino={} usuario={}", grupo, req.topico(), req.destino(), usuario);
        return calcularLag(grupo);
    }

    // ------------------------------------------------------------------ apoyo

    private static void agregar(List<AlterConfigOp> cambios, String clave, Object valor) {
        if (valor != null) {
            cambios.add(new AlterConfigOp(new ConfigEntry(clave, String.valueOf(valor)), AlterConfigOp.OpType.SET));
        }
    }

    private TopicoResponse aRespuesta(TopicDescription d, Map<String, String> configs) {
        List<ParticionInfo> particiones = d.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new ParticionInfo(p.partition(),
                        p.leader() != null ? p.leader().id() : null,
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        int replicas = d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
        return new TopicoResponse(d.name(), d.partitions().size(), replicas, props.esProtegido(d.name()), configs, particiones);
    }

    private static ConsumerGroupResponse aRespuesta(ConsumerGroupDescription d) {
        List<ConsumerGroupResponse.MiembroInfo> miembros = d.members().stream()
                .map(m -> new ConsumerGroupResponse.MiembroInfo(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .map(tp -> tp.topic() + "-" + tp.partition())
                                .sorted()
                                .toList()))
                .toList();
        return new ConsumerGroupResponse(d.groupId(), String.valueOf(d.state()),
                d.coordinator() != null ? d.coordinator().id() : null, d.partitionAssignor(), miembros);
    }

    /** Espera el resultado con timeout y traduce los errores de Kafka a errores de la API. */
    private <T> T esperar(KafkaFuture<T> futuro, String operacion) {
        try {
            return futuro.get(props.timeoutSegundos(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperacionKafkaException("Operacion interrumpida: " + operacion, e);
        } catch (TimeoutException e) {
            throw new OperacionKafkaException("Kafka no respondio a tiempo al " + operacion, e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            if (causa instanceof UnknownTopicOrPartitionException) {
                throw new RecursoNoEncontradoException("No existe el topico indicado (" + operacion + ")");
            }
            if (causa instanceof GroupIdNotFoundException) {
                throw new RecursoNoEncontradoException("No existe el consumer group indicado (" + operacion + ")");
            }
            if (causa instanceof TopicExistsException) {
                throw new ConflictoException("El topico ya existe");
            }
            if (causa instanceof GroupNotEmptyException) {
                throw new ConflictoException("El consumer group tiene consumidores activos: detengalos antes de resetear");
            }
            if (causa instanceof InvalidPartitionsException || causa instanceof InvalidReplicationFactorException
                    || causa instanceof InvalidConfigurationException || causa instanceof InvalidRequestException
                    || causa instanceof InvalidTopicException || causa instanceof PolicyViolationException) {
                throw new IllegalArgumentException(causa.getMessage());
            }
            throw new OperacionKafkaException("Error de Kafka al " + operacion + ": " + causa.getMessage(), causa);
        }
    }
}
