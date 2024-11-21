# Dropwizard-Guiciest (Dropwizard with Guice)

## About

DPC software is based upon a combination of JDK, Jersey, Guice, Dropwizard, and Hibernate.

To update the various dependencies of the software required the removal of all software that was dependent on javax (rather than Guice or Jakarta). To achieve this, DPC has cloned the [Flipkart dropwizard-guicier project](https://flipkart-incubator.github.io/dropwizard-guicier/), which itself is a fork of [HubSpot/dropwizard-guicier](https://github.com/HubSpot/dropwizard-guicier) (with embedded dependency [Squarespace/jersey2-guice](https://github.com/Squarespace/jersey2-guice)).

The project has been updated to be jakarta-only and to support the specific DI needs of the DPC project.
  * Significant reduction in dependency footprint
  * Reduction in inter-dependency conflicts (hence, lesser chances of [jar hell](https://dzone.com/articles/what-is-jar-hell))
  * Compatibility with latest versions of Dropwizard

## Usage
Add this library as dependency:

```xml
<dependencies>
    <dependency>
        <groupId>gov.cms.dpc</groupId>
        <artifactId>dropwizard-guiciest</artifactId>
        <version>${dropwizard-guiciest-version}</version>
    </dependency>
</dependencies>
```

## Related links
 * [**Dropwizard with Guice** on mvnrepository](https://mvnrepository.com/artifact/com.flipkart.utils/dropwizard-guicier)
 * [**Dropwizard with Guice** on Maven Central](https://search.maven.org/artifact/com.flipkart.utils/dropwizard-guicier)
