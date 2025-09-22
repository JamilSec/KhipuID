# KhipuID

KhipuID es un proyecto en **Java** basado en **Arquitectura Limpia (Clean Architecture)** que permite consultar información de contribuyentes peruanos en **SUNAT** (RUC/DNI).  

El sistema organiza sus capas en **domain**, **application**, **infrastructure** y **runner**, siguiendo buenas prácticas de **DDD (Domain-Driven Design)**.  
Su objetivo es ofrecer una solución modular, extensible y desacoplada para la verificación de identidad y consumo de datos tributarios en Perú.

---

## Características

- Consulta por **RUC** o **DNI** en la web de SUNAT.  
- Conversión de **HTML → JSON estructurado**, con soporte para snake_case.  
- Organización en capas (domain, application, infrastructure, runner).  
- Uso de patrones de **Clean Architecture** y **DDD**.  
- Dependencias modernas: `Jsoup` para parsing y `Jakarta JSON-B` para serialización.  
- Preparado para integrarse como microservicio o librería.  

---

## Arquitectura

```
src/main/java/com/jamilsec/khipuid
├── application          # Casos de uso (ej. ConsultarHtmlUseCase)
├── domain
│   ├── model            # Entidades: Ruc, Dni, HtmlDocumento
│   └── ports            # Interfaces (ej. HtmlConsultaGateway)
├── infrastructure
│   └── sunat            # Adaptadores: HttpClient, Parsers, TokenProvider
└── runner               # Entry points (ej. ConsoleRunner)
```

---

## Dependencias principales

- [Java 17+](https://adoptium.net/) (probado con JDK 21 y 24).  
- [Maven](https://maven.apache.org/) como gestor de build.  
- [Jsoup](https://jsoup.org/) para scraping/parsing HTML.  
- [Jakarta JSON-B](https://projects.eclipse.org/projects/ee4j.jsonb) con **Yasson** como implementación.  

```xml
<dependencies>
    <!-- Jsoup -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.21.2</version>
    </dependency>

    <!-- Jakarta JSON-B API -->
    <dependency>
        <groupId>jakarta.json.bind</groupId>
        <artifactId>jakarta.json.bind-api</artifactId>
        <version>3.0.1</version>
    </dependency>

    <!-- Yasson (implementación JSON-B) -->
    <dependency>
        <groupId>org.eclipse</groupId>
        <artifactId>yasson</artifactId>
        <version>3.0.3</version>
    </dependency>
</dependencies>
```

---

## Ejecución

### Consola
Ejemplo rápido usando el `ConsoleRunner`:

```bash
mvn clean package
java -cp target/KhipuID-1.0-SNAPSHOT.jar com.jamilsec.khipuid.runner.ConsoleRunner 20100047218
```

### Ejemplo de salida JSON

```json
{
   "ruc": "20100047218",
   "razon_social": "BANCO DE CREDITO DEL PERU",
   "estado": "ACTIVO",
   "condicion": "HABIDO",
   "domicilio_fiscal": "JR. CENTENARIO NRO. 156 ...",
   "actividades_economicas": [
      "PRINCIPAL - 6419 - OTROS TIPOS DE INTERMEDIACIÓN MONETARIA",
      "SECUNDARIA 1 - 6491 - ARRENDAMIENTO FINANCIERO"
   ],
   "fecha_consulta": "22/09/2025 17:52"
}
```

---

## Roadmap

- [ ] Implementar consulta de representantes legales.  
- [ ] Exponer como API REST con Spring Boot.  
- [ ] Añadir pruebas unitarias y de integración.  
- [ ] Contenedorización con Docker.  

---

## Licencia

Este proyecto está bajo la licencia **MIT**.  
Consulta el archivo [LICENSE](LICENSE) para más detalles.
