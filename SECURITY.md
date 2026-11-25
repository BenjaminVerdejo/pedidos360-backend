# Politica de seguridad - Pedidos360

## Versiones con soporte

| Version | Soporte de seguridad |
|---|---|
| rama `main` (ultima version desplegada) | Si |
| versiones anteriores | No |

## Reportar una vulnerabilidad

No abras un *issue* publico. Usa **GitHub > Security > Report a vulnerability** (reporte privado)
o escribe al equipo indicando: componente afectado, pasos para reproducir e impacto estimado.

| Etapa | Plazo objetivo |
|---|---|
| Acuse de recibo | 2 dias habiles |
| Clasificacion (CVSS v3.1) | 5 dias habiles |
| Correccion CRITICA / ALTA | 7 / 30 dias |
| Correccion MEDIA / BAJA | proxima iteracion / segun disponibilidad |

Los incidentes que afecten datos personales se gestionan segun la Ley 21.719 (proteccion de datos
personales) y los de ciberseguridad segun la Ley 21.663 (Ley Marco de Ciberseguridad), incluyendo la
notificacion a las autoridades y a los titulares de los datos cuando corresponda.

## Controles automatizados

Cada *push* y *pull request* ejecuta el pipeline [`ci-seguridad.yml`](.github/workflows/ci-seguridad.yml):
pruebas + cobertura, SAST (Semgrep + CodeQL), deteccion de secretos (Gitleaks), SCA (Trivy),
analisis de imagenes Docker (Hadolint + Trivy), DAST (OWASP ZAP + pruebas dinamicas) y rendimiento (k6).
Detalle en [docs/seguridad-y-calidad.md](docs/seguridad-y-calidad.md).
