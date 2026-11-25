# Plan de mantenimiento - Pedidos360

Objetivo: que cada nueva iteracion del software conserve la calidad, la seguridad y el cumplimiento
normativo alcanzados en la EP5. Se basa en ISO/IEC 14764 (tipos de mantenimiento) e ISO/IEC 25010
(caracteristicas de calidad).

## 1. Tipos de mantenimiento y como se aplican

| Tipo (ISO/IEC 14764) | Disparador | Proceso en Pedidos360 | Control automatizado |
|---|---|---|---|
| Correctivo | Defecto reportado o prueba fallida | Issue -> rama -> prueba que reproduce el defecto -> correccion -> PR | Pipeline completo; la prueba nueva evita la regresion |
| Preventivo | Ejecucion programada semanal, Dependabot | Actualizar dependencias e imagenes base antes de que el CVE sea explotado | `schedule` del pipeline + Dependabot |
| Adaptativo | Cambio de entorno: nueva version de Java/Spring/Angular, cambio legal | PR con la migracion; se revisa la matriz normativa | Pruebas + SCA + DAST validan la migracion |
| Perfectivo | Nuevos requerimientos o mejoras de rendimiento | Historia de usuario con criterios de aceptacion y casos de prueba | Pruebas + k6 (no degradar p95) |

## 2. Calendario

| Frecuencia | Actividad | Responsable |
|---|---|---|
| Cada commit / PR | Pipeline DevSecOps completo + checklist de la plantilla de PR | Desarrollador + revisor |
| Semanal (lunes) | Re-escaneo programado (SCA, imagenes); revisar PR de Dependabot | Encargado de DevSecOps |
| Mensual | Revisar alertas de GitHub Security, rotar credenciales de RabbitMQ/RDS, revisar logs de auditoria | Lider tecnico |
| Trimestral | Revisar matriz normativa (Ley 21.719 / 21.663), roles de Azure AD y permisos; prueba de restauracion de backup RDS | Lider tecnico + responsable de datos |
| Por version mayor | Pentest manual / ZAP full scan autenticado; revision de arquitectura | Equipo QA / seguridad |

## 3. Indicadores (KPI) para preservar la calidad

| Indicador | Meta | Fuente |
|---|---|---|
| Pruebas automatizadas aprobadas | 100 % | Job "Pruebas" |
| Cobertura de lineas (pedidos-service) | >= 70 % y sin bajar entre versiones | JaCoCo |
| Hallazgos SAST severidad ERROR | 0 | Semgrep / CodeQL |
| CVE CRITICAL/HIGH con parche abiertos | 0 CRITICAL; HIGH < 30 dias | Trivy / Dependabot |
| Tiempo medio de correccion (MTTR) vulnerabilidad critica | <= 7 dias | GitHub Security |
| p95 de tiempo de respuesta | < 500 ms | k6 |
| Secretos expuestos | 0 | Gitleaks |

## 4. Gestion de cambios y cumplimiento

1. Todo cambio entra por *pull request*; la rama `main` debe protegerse (Settings > Branches) exigiendo
   el pipeline en verde y al menos una revision.
2. Los datos personales (nombre, email del cliente) solo se usan para gestionar el pedido; los logs los
   enmascaran. Ante una brecha se activa el procedimiento de `SECURITY.md` (Ley 21.719: notificacion a la
   Agencia de Proteccion de Datos y a los titulares; Ley 21.663: reporte al CSIRT cuando corresponda).
3. Las credenciales viven en variables de entorno / AWS Secrets Manager, nunca en el repositorio.
4. Cada version se etiqueta (tag) y la imagen Docker se reconstruye desde cero para incorporar los
   parches del sistema base.
