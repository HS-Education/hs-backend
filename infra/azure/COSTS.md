# Estimación previa, sin aprovisionar

## Región seleccionada: Mexico Central

La política Students de esta cuenta bloquea East US/East US 2. Consulta oficial de tarifas del 29/09/2026 para Mexico Central, 730 h/mes, sin descuentos ni HA:

| Recurso | Tarifa | Estimación mensual |
| --- | --- | --- |
| App Service Linux B2 compartido | US$0,0374/h | US$27,30 |
| PostgreSQL Flexible B1ms | US$0,0187/h | US$13,65 |
| PostgreSQL Premium LRS 32 GB | US$0,1265/GB-mes | US$4,05 |
| Service Bus Standard, base | US$0,014785/h | US$10,79 |
| Base estimada | Antes de uso variable y beneficios | **US$55,79** |

Reservar aproximadamente **US$60–70/mes para uso pequeño**, sin OpenRouter; no es un tope. La base supera los US$50 iniciales y no demuestra los beneficios gratuitos reales de Students. El budget de US$60 avisa, no detiene recursos. El crédito publicado es de US$100 por 12 meses, no mensual. Confirmar saldo efectivo antes de crear recursos pagos.

West US se consultó como alternativa permitida: B2 US$0,036/h, B1ms US$0,022/h y almacenamiento US$0,138/GB-mes, sin una mejora clara frente a Mexico Central. No se eligió por una latencia medida.

## Referencia previa: East US, no desplegable en esta cuenta

Consulta de tarifas públicas USD para **East US**, 29/09/2026. Supuesto: 730 horas/mes, una instancia del plan, sin HA, sin slots y sin descuentos. No es una factura ni una garantía de capacidad o disponibilidad regional.

| Recurso | Tarifa consultada | Estimación mensual |
| --- | --- | --- |
| App Service Linux B2 compartido por Java/Angular y Python | US$0,034/h | US$24,82 |
| PostgreSQL Flexible B1ms | US$0,017/h | US$12,41 |
| PostgreSQL Premium LRS 32 GB | US$0,115/GB-mes | US$3,68 |
| Service Bus Standard | medidor mensual US$10; medidor horario US$0,013441 | ≈ US$10 |
| Base estimada | sin sumar dos veces el medidor de Service Bus | **≈ US$50,91** |

Blob depende de GB, lecturas y escrituras; Key Vault tiene medidor de operaciones Standard de US$0,03/10.000 operaciones; Log Analytics muestra US$2,30/GB ingerido de pago y tramos gratuitos. Reservar **US$55–65/mes como orientación para uso pequeño**, no como límite máximo. Egreso, copias adicionales, operaciones, logs y OpenRouter pueden elevarlo. OpenRouter se paga con su cuota independiente y no está incluido. Los medidores pueden variar; actualizar antes de crear:

```powershell
./infra/azure/Get-RetailPrices.ps1
```

La oferta Students publica US$100 de crédito durante 12 meses; **no son US$100 cada mes**. No hemos leído el saldo disponible ni demostrado qué servicios gratuitos específicos cubre esta cuenta. Confirmar beneficios efectivos y medidores antes de decidir cuánto crédito se consumirá. Con precios de lista y US$100 íntegros, este conjunto 24/7 consumiría el crédito en aproximadamente dos meses; no presupuestar un año gratuito.

El spending limit de Students debe permanecer **On**. Nunca actualizar a pago por uso ni usar otra suscripción como alternativa automática. Un budget envía avisos, pero no limita por sí mismo el gasto. Detener una Web App no deja de facturar su App Service Plan. PostgreSQL detenido conserva costos de almacenamiento y su detención tiene duración máxima; planificar ventanas de demostración y eliminación de recursos únicamente después de respaldar y con aprobación.

Fuentes: [API oficial de tarifas](https://learn.microsoft.com/en-us/rest/api/cost-management/retail-prices/azure-retail-prices), [oferta Students](https://azure.microsoft.com/en-us/pricing/offers/ms-azr-0170p), [facturación de planes App Service](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans).
