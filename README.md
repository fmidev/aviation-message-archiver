<!-- Generated - DO NOT EDIT! Instead, edit sources under src/doc directory. -->
<!--
  After editing the source file or changing pom.xml, run at project root:
  mvn resources:copy-resources@readme
-->

# Aviation Message Archiver

An application that reads aviation message files, parses some basic properties and stores messages along with parsed
metadata in a database.

## Overview

Aviation Message Archiver is a Spring Boot service application. It monitors configured input directories for aviation
message files. Whenever new files appear, it scans for messages in files, parses some basic properties like message
(issued) time, validity period (when applicable), station / location indicator. It stores each message in a PostGIS
(PostgreSQL) database.

## Contents

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Overview](#overview)
- [Contents](#contents)
- [Feature overview](#feature-overview)
    - [Supported message types and formats](#supported-message-types-and-formats)
- [Getting started](#getting-started)
- [Logging](#logging)
    - [Processing identifier and phase](#processing-identifier-and-phase)
    - [Processing context](#processing-context)
    - [Processing statistics](#processing-statistics)
    - [Structured logging](#structured-logging)
- [Application configuration](#application-configuration)
    - [Products](#products)
    - [Message populators](#message-populators)
        - [Bundled message populators](#bundled-message-populators)
            - [BulletinHeadingDataPopulator](#bulletinheadingdatapopulator)
            - [FileMetadataPopulator](#filemetadatapopulator)
            - [FileNameDataPopulator](#filenamedatapopulator)
            - [FixedDurationValidityPeriodPopulator](#fixeddurationvalidityperiodpopulator)
            - [FixedProcessingResultPopulator](#fixedprocessingresultpopulator)
            - [FixedRoutePopulator](#fixedroutepopulator)
            - [FixedTypePopulator](#fixedtypepopulator)
            - [MessageContentTrimmer](#messagecontenttrimmer)
            - [MessageDataPopulator](#messagedatapopulator)
            - [MessageDiscarder](#messagediscarder)
            - [MessageFutureTimeValidator](#messagefuturetimevalidator)
            - [MessageMaximumAgeValidator](#messagemaximumagevalidator)
            - [ProductMessageTypesValidator](#productmessagetypesvalidator)
            - [StationIcaoCodeReplacer](#stationicaocodereplacer)
            - [StationIdPopulator](#stationidpopulator)
        - [Conditional message popular activation](#conditional-message-popular-activation)
            - [Activation property](#activation-property)
            - [Activation operator and operand](#activation-operator-and-operand)
    - [Identifier mappings](#identifier-mappings)
    - [Spring Boot configuration properties](#spring-boot-configuration-properties)
- [License](#license)

<!-- /TOC -->

## Feature overview

* [Configurable](#application-configuration) archival process.
* Automatic [message type](#supported-message-types-and-formats) recognition.
* Supports TAC (experimental) and IWXXM [message formats](#supported-message-types-and-formats).
* Supported file formats:
    * WMO GTS meteorological message as specified
      by [WMO Doc. 386](https://library.wmo.int/index.php?lvl=notice_display&id=21811).
        * Multiple meteorological messages may be included in a file.
        * A supported COLLECT document is also supported as message content.
    * [COLLECT 1.2](https://schemas.wmo.int/collect/1.2/) document
    * A meteorological bulletin starting with a WMO GTS abbreviated heading line, followed by messages or a COLLECT
      document
    * A single message (not recommended for TAC messages)
* Supported database engines:
    * [PostGIS](https://postgis.net/) ([PostgreSQL](https://www.postgresql.org/))
    * [H2](https://h2database.com/) (for testing)
* Traceable [logging](#logging). Support for structured JSON logging.
* Built on [Spring Boot](https://spring.io/projects/spring-boot)
  and [Spring Integration](https://spring.io/projects/spring-integration).

### Supported message types and formats

Supported message types and formats are listed in the table below. Generally, this application supports all

* TAC format message types that are supported
  by [fmi-avi-messageconverter-tac](https://github.com/fmidev/fmi-avi-messageconverter-tac)
  library [`TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO`](https://github.com/fmidev/fmi-avi-messageconverter-tac/blob/fmi-avi-messageconverter-tac-5.0.0/src/main/java/fi/fmi/avi/converter/tac/conf/TACConverter.java)
  conversion and
* IWXXM format message types that are supported
  by [fmi-avi-messageconverter-iwxxm](https://github.com/fmidev/fmi-avi-messageconverter-iwxxm)
  library [`IWXXM_STRING_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO`](https://github.com/fmidev/fmi-avi-messageconverter-iwxxm/blob/fmi-avi-messageconverter-iwxxm-4.0.0/src/main/java/fi/fmi/avi/converter/iwxxm/conf/IWXXMConverter.java)
  conversion.

| Message type              | TAC | IWXXM 2.1 | IWXXM 3.0 |
|---------------------------|:---:|:---------:|:---------:|
| METAR                     | 1\) |    \+     |    \+     |
| SPECI                     | 1\) |    \+     |    \+     |
| TAF                       | 1\) |    \+     |    \+     |
| SIGMET                    | \-  |    \+     |    \+     |
| AIRMET                    | \-  |    \+     |    \+     |
| Volcanic Ash Advisory     | 1\) |    n/a    |    \+     |
| Tropical Cyclone Advisory | \-  |    n/a    |    \+     |
| Space Weather Advisory    | 1\) |    n/a    |    \+     |

\+ Complete support  
\- Unsupported  
1\) Experimental

## Getting started

The next steps guide you to test the application with example [configuration](#application-configuration)
using H2 (in-memory) or PostGIS database engine.

1. After cloning the code repository, build the application with [Maven](https://maven.apache.org/).

  ```shell
  mvn package
  ```

2. Set up the database engine.
    * **H2:** is automatically set up at application startup, no actions needed.
    * **PostGIS:** Database is easily set up with Docker or Podman. Use credentials specified by `spring.datasource.*`
      properties in the [application.yml] configuration for profile `local & postgresql & !openshift`.
      ```shell
      docker run \
        -p 127.0.0.1:5432:5432 \
        --env POSTGRES_USER=avidb_agent \
        --env POSTGRES_PASSWORD=secret \
        --env POSTGRES_DB=avidb \
        --name avidb \
        docker.io/postgis/postgis:14-3.2
      ```

   In the `local` mode used in this guide, the application will automatically initialize
   the [schema](https://github.com/fmidev/avidb-schema) at startup.

3. Prepare an SQL script to populate the `avidb_stations` table. This is optional for testing the application, but all
   messages will be rejected without a matching location indicator in the `icao` column of `avidb_stations` table.
    * **H2:**
      See [schema-h2.sql](https://github.com/fmidev/avidb-schema/blob/avidb-schema-1.0.0/h2/schema-h2.sql)
      for the schema, and [h2-data/example/avidb_stations.sql](src/main/resources/h2-data/example/avidb_stations.sql)
      for an insertion template.
    * **PostGIS:**
      See [schema-postgresql.sql](https://github.com/fmidev/avidb-schema/blob/avidb-schema-1.0.0/postgresql/schema-postgresql.sql)
      for the schema,
      and [postgresql-data/example/avidb_stations.sql](src/main/resources/postgresql-data/example/avidb_stations.sql)
      for an insertion template.

4. Start the application. Replace
    * `$AVIDB_STATIONS_SQL` with a path to the file created in previous step, or omit
      the `spring.sql.init.data-locations` property.
    * `$DB_ENGINE` with `h2` or `postgresql`.

   ```shell
   java \
     -Dspring.profiles.active="local,example,$DB_ENGINE" \
     -Dspring.sql.init.data-locations="\${example.spring.sql.init.data-locations.$DB_ENGINE},file://$AVIDB_STATIONS_SQL" \
     -jar target/aviation-message-archiver-1.0.0-bundle.jar
   ```

5. Check
   some [actuator endpoints](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/actuator.html#actuator.endpoints)
   to see that the application is running and healthy.
    * info: <http://localhost:8080/actuator/info>
    * health: <http://localhost:8080/actuator/health>

6. Copy some message files in the input directories specified by the `production-line.products[n].input-dir` properties
   in the [application.yml] configuration file.

7. After an input file is processed, the application moves it to one of target directories specified by
   the `production-line.products[n].archive-dir` and `production-line.products[n].fail-dir` properties in
   the [application.yml] configuration file. The processing identifier is appended to the file name.

8. Connect to the database.
    * **H2:** You can access the H2 database console at <http://localhost:8080/h2-console/login.jsp> with default
      connection settings and credentials.
    * **PostGIS:** Use `psql` or any appropriate client with connection information provided in the database setup step.

   Look at the archived and rejected message tables in the database for any messages. E.g.

  ```sql
SELECT *
FROM avidb_messages AS messages
         LEFT JOIN avidb_message_iwxxm_details AS iwxxm ON iwxxm.message_id = messages.message_id
ORDER BY message_time DESC;
SELECT *
FROM avidb_rejected_messages AS r_messages
         LEFT JOIN avidb_rejected_message_iwxxm_details as r_iwxxm
                   ON r_iwxxm.rejected_message_id = r_messages.rejected_message_id
ORDER BY message_time DESC;
  ```

## Logging

Application logging is designed to enable tracking of file processing and possible errors during the process. This
section describes implemented mechanisms supporting this goal.

### Processing identifier and phase

**Processing identifier** is a unique string within a single Java virtual machine that identifies a single processing of
a single input file. It connects all separate log entries of the file processing. Processing identifier `0` or `null`
denotes that a processing identifier is not available or applicable on the log entry.
See [FileProcessingIdentifier](src/main/java/fi/fmi/avi/archiver/file/FileProcessingIdentifier.java) class for details.

**Processing phase** describes the phase in the processing chain. Processing phase `nul` or `null` denotes that the
processing phase information is not available or applicable on the log entry.
See [ProcessingPhase](src/main/java/fi/fmi/avi/archiver/logging/model/ProcessingPhase.java) enum for list of phases.

When using unstructured logging, the processing identifier and phase are logged before logging level in format:

```
[<processing-id>:<phase>] <level> ...
```

### Processing context

Most log entries contain processing context information, referring to file being processed, bulletin of the file and
message within the bulletin. The format of the processing context in a log message is:

```
<fileReference:bulletinReference:messageReference>
```

which can be extracted to:

```
<productId/fileName:bulletinIndex(bulletinHeading)@bulletinCharIndex:messageIndex(messageExcerpt)>
```

where

* **productId:** identifier of the [product](#products) specifying the file under processing
* **fileName:** name of the file under processing
* **bulletinIndex:** index of bulletin within file, starting from 0
* **bulletinHeading:** heading (GTS or collect identifier) of the bulletin under processing
* **bulletinCharIndex:** character index of bulletin within file, starting from 0
* **messageIndex:** index of message within bulletin, starting from 0
* **messageExcerpt:** excerpt from beginning of a TAC message, or IWXXM message `gml:id`

Unavailable or inapplicable fields and separators are omitted.

See [ReadableLoggingContext](src/main/java/fi/fmi/avi/archiver/logging/model/ReadableLoggingContext.java) interface for
more information.

### Processing statistics

When file processing is finished, the application logs statistics of the processing in the following format:

```
M{X:n,...,T:n} B{X:1,...,T:n} F{X}
```

where

* `M{}` contains statistics counted as messages within the file.
* `B{}` contains statistics counted as bulletins within the file.
* `F{}` contains the overall result of the file processing.
* `X:n` contains the count of items having the specified processing result. Any processing results with zero (`0`) count
  are omitted.
    * `X` is the abbreviated one-letter name of the processing result.
      See [ReadableFileProcessingStatistics.ProcessingResult](src/main/java/fi/fmi/avi/archiver/logging/model/ReadableFileProcessingStatistics.java)
      enum for possible values.
    * `n` is the count of items resulting with the preceding processing result.
* `T:n` contains the total count `n` of processed items.

Aggregated results denote the worst processing result of a message within the aggregation context. E.g. a bulletin is
considered rejected when it contains at least one rejected message, but no failed messages.

See [ReadableFileProcessingStatistics](src/main/java/fi/fmi/avi/archiver/logging/model/ReadableFileProcessingStatistics.java)
interface for more information.

### Structured logging

Structured JSON logging can be enabled by activating the `logstash` runtime profile in the startup command. E.g.

```shell
java -Dspring.profiles.active=<other profiles...>,logstash ...
```

## Application configuration

Application configuration properties are collected in a YAML file called [application.yml]. The provided configuration
file is a base configuration, acting as an example. You can use it as a base for your own application configuration
file. In your custom configuration file you need to add and/or override only changed or forced properties in your own
configuration file, because the provided base configuration file is loaded as well.
See [External Application Properties](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/features.html#features.external-config.files)
in Spring Boot reference documentation for instructions on how to apply your custom configuration file.

Runtime behavior is controlled
using [Spring profiles](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/features.html#features.profiles)
which are activated by the application launch command. Profiles declared in the provided configuration are described in
the [application.yml] file.

The most relevant part of the configuration file is the production line configuration under `production-line` property.
Its contents are described below.

### Products

Aviation product model describes basic information like input and output directories and accepted file name patterns.
This application can be configured for one or more products. Template of products configuration:

```yaml
production-line:
  products:
    - id: <product id>
      route: <route name>
      input-dir: <input directory>
      archive-dir: <archived files directory>
      fail-dir: <directory for failed files>
      files:
        - pattern: <file name regex pattern>
          name-time-zone: <zone of timestamp in file name>
          format: <message format>
        - ...
    - ...
```

See [application.yml] file for a documented configuration example. More detailed documentation on individual properties
can be found in the [AviationProduct](src/main/java/fi/fmi/avi/archiver/config/model/AviationProduct.java) model class.

### Message populators

[Message populators](src/main/java/fi/fmi/avi/archiver/message/populator/MessagePopulator.java) are small classes that
are responsible for populating the archive data object with message data parsed from the input file. Each message
populator focuses on a single responsibility, and together all configured populators construct the complete message
entity to be archived. Message populators also decide whether a message is considered

* **eligible for archival**, thus will be stored in the message database table after all populators are executed,
* **rejected**, thus will be stored in the rejected message database table after all populators are executed,
* **discarded**, thus will be ignored immediately and logged at info level,
* **failed**, thus will be ignored immediately and logged at error level.

Message populator configuration specifies which populators are executed and in which order. A message populator executed
later in the execution chain may then override values set by previously executed populators. The configuration applies
to all [products](#products). Any populator may be configured to execute conditionally.

Template of message populators configuration:

```yaml
production-line:
  message-populators:
    - # Name of message populator to execute (mandatory)
      name: <populator name>
      # Optional activation conditions. The specified populator is executed only when all of
      # provided activation conditions are satisfied. Omit to execute unconditionally.
      activate-on:
        <activation property>:
          <activation operator>: <activation operand>
          ...
        ...
      # Map of populator-specific configuration options. Some of them may be mandatory.
      # May be omitted, when no configuration options are given.
      config:
        <config property name>: <config property value>
        ...
    - ...
```

Message populator name is generally by convention the same as the class simple name. For example, name
of `fi.fmi.avi.archiver.message.populator.MessageDataPopulator` class is `MessageDataPopulator` in the configuration.

A base configuration is provided in the [application.yml] file as an example.

#### Bundled message populators

This application comes with handful of bundled message populators. Some of them,
like [MessageDataPopulator](#messagedatapopulator), play an essential role in the archival process. The
provided [application.yml] has an example configuration of these. Others,
like [FixedProcessingResultPopulator](#fixedprocessingresultpopulator)
or [StationIcaoCodeReplacer](#stationicaocodereplacer), are provided for customized message handling, along with the
possibility for [conditional activation](#conditional-message-popular-activation). One message
populator, [StationIdPopulator](#stationidpopulator), cannot be configured, but is implicitly active.

Available populators are listed below (in alphabetic order by name). These are declared for use in
the [MessagePopulatorFactoryConfig](src/main/java/fi/fmi/avi/archiver/config/MessagePopulatorFactoryConfig.java) class.

##### BulletinHeadingDataPopulator

Populate properties parsed from input bulletin heading.

* **name:**
  [BulletinHeadingDataPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/BulletinHeadingDataPopulator.java)
* **config:**
    * `bulletin-heading-sources` (optional) - List of bulletin heading sources in preferred order.  
      Available values are specified
      in [BulletinHeadingSource](src/main/java/fi/fmi/avi/archiver/message/populator/BulletinHeadingSource.java)
      enum.  
      Example:
      ```yaml
      bulletin-heading-sources:
        - GTS_BULLETIN_HEADING
        - COLLECT_IDENTIFIER
      ```

##### FileMetadataPopulator

Populate properties available in input file metadata.

* **name:**
  [FileMetadataPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/FileMetadataPopulator.java)
* **config:** ~

##### FileNameDataPopulator

Populate properties parsed from file name.

* **name:**
  [FileNameDataPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/FileNameDataPopulator.java)
* **config:** ~

##### FixedDurationValidityPeriodPopulator

Set validity period to a fixed duration period starting from message time.

* **name:**
  [FixedDurationValidityPeriodPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/FixedDurationValidityPeriodPopulator.java)
* **config:**
    * `validity-end-offset` (mandatory) - Validity period length from message time as ISO 8601
      duration ([java.time.Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html)).  
      Example:
      ```yaml
      validity-end-offset: PT24H
      ```

##### FixedProcessingResultPopulator

Set processing result to specified value.

* **name:**
  [FixedProcessingResultPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/FixedProcessingResultPopulator.java)
* **config:**
    * `result` (mandatory) - Processing result code name. Available values are specified
      in [ProcessingResult](src/main/java/fi/fmi/avi/archiver/message/ProcessingResult.java) enum.  
      Example:
      ```yaml
      result: FORBIDDEN_MESSAGE_STATION_ICAO_CODE
      ```

##### FixedRoutePopulator

Set route to specified value.

* **name:**
  [FixedRoutePopulator](src/main/java/fi/fmi/avi/archiver/message/populator/FixedRoutePopulator.java)
* **config:**
    * `route` (mandatory) - Route name.  
      Available values are specified in the `production-line.route-ids` [identifier mapping](#identifier-mappings)
      property.  
      Example:
      ```yaml
      route: DEFAULT
      ```

##### FixedTypePopulator

Set message type to specified value.

* **name:**
  [FixedTypePopulator](src/main/java/fi/fmi/avi/archiver/message/populator/FixedTypePopulator.java)
* **config:**
    * `type` (mandatory) - Message type name.  
      Available values are specified in the `production-line.type-ids` [identifier mapping](#identifier-mappings)
      property.  
      Example:
      ```yaml
      type: METAR
      ```

##### MessageContentTrimmer

Trim whitespaces around message content.

* **name:**
  [MessageContentTrimmer](src/main/java/fi/fmi/avi/archiver/message/populator/MessageContentTrimmer.java)
* **config:** ~

##### MessageDataPopulator

Populate properties parsed from message content.

* **name:**
  [MessageDataPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/MessageDataPopulator.java)
* **config:**
    * `message-type-location-indicator-types` (optional) - Message type-specific list of location indicator types in
      order of preference for reading the station ICAO code.  
      Available message types are specified in the map property `production-line.type-ids`. Available location indicator
      types are specified
      in [GenericAviationWeatherMessage.LocationIndicatorType](https://github.com/fmidev/fmi-avi-messageconverter/blob/fmi-avi-messageconverter-6.0.0/src/main/java/fi/fmi/avi/model/GenericAviationWeatherMessage.java)
      enum.  
      Example:
      ```yaml
      message-type-location-indicator-types:
        - AIRMET:
            - ISSUING_AIR_TRAFFIC_SERVICES_REGION
        - METAR:
            - AERODROME
        - SIGMET:
            - ISSUING_AIR_TRAFFIC_SERVICES_REGION
        - SPECI:
            - AERODROME
        - SPACE_WEATHER_ADVISORY: [ ]
        - TAF:
            - AERODROME
        - TROPICAL_CYCLONE_ADVISORY: [ ]
        - VOLCANIC_ASH_ADVISORY: [ ]
      ```
    * `default-location-indicator-types` (optional) - Default list of location indicator types in order of preference
      for reading the station ICAO code.  
      Only used when the message type-specific list is not configured. Available location indicator types are specified
      in [GenericAviationWeatherMessage.LocationIndicatorType](https://github.com/fmidev/fmi-avi-messageconverter/blob/fmi-avi-messageconverter-6.0.0/src/main/java/fi/fmi/avi/model/GenericAviationWeatherMessage.java)
      enum.  
      Example:
      ```yaml
      default-location-indicator-types:
        - AERODROME
        - ISSUING_AIR_TRAFFIC_SERVICES_REGION
      ```

##### MessageDiscarder

Discard message.

* **name:**
  [MessageDiscarder](src/main/java/fi/fmi/avi/archiver/message/populator/MessageDiscarder.java)
* **config:** ~

##### MessageFutureTimeValidator

Reject message if message time is too far in the future.

* **name:**
  [MessageFutureTimeValidator](src/main/java/fi/fmi/avi/archiver/message/populator/MessageFutureTimeValidator.java)
* **config:**
    * `accept-in-future` (mandatory) - Maximum duration as ISO 8601
      duration ([java.time.Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html)) from current
      time to accepted message time.  
      Example:
      ```yaml
      accept-in-future: PT12H
      ```

##### MessageMaximumAgeValidator

Reject message if message time is too far in the past.

* **name:**
  [MessageMaximumAgeValidator](src/main/java/fi/fmi/avi/archiver/message/populator/MessageMaximumAgeValidator.java)
* **config:**
    * `confname` (mandatory) - Maximum duration as ISO 8601
      duration ([java.time.Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html)) until current
      time to accepted message time.  
      Example:
      ```yaml
      maximum-age: PT36H
      ```

##### ProductMessageTypesValidator

Reject message if message type is not one of the valid types configured for the product.

* **name:**
  [ProductMessageTypesValidator](src/main/java/fi/fmi/avi/archiver/message/populator/ProductMessageTypesValidator.java)
* **config:**
    * `confname` (mandatory) - Product-specific list of valid message types.  
      Available products are specified under list property `production-line.products`. Available message types are
      specified in map property `production-line.type-ids`.  
      Example:
      ```yaml
      product-message-types:
        example1:
          - METAR
          - SPECI
        example2:
          - TAF
          - SIGMET
          - AIRMET
          - TROPICAL_CYCLONE_ADVISORY
          - VOLCANIC_ASH_ADVISORY
          - SPACE_WEATHER_ADVISORY
      ```

##### StationIcaoCodeReplacer

Replace all occurrences of regular expression pattern in the station ICAO code with the provided replacement.

* **name:**
  [StationIcaoCodeReplacer](src/main/java/fi/fmi/avi/archiver/message/populator/StationIcaoCodeReplacer.java)
* **config:**
    * `pattern` (mandatory) - Pattern to find in station ICAO code.  
      Pattern syntax is specified
      in [java.util.regex.Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) class.  
      Example:
      ```yaml
      pattern: '^XY([A-Z]{2})$'
      ```
    * `replacement` (mandatory) - Replacement string.  
      Replacement syntax is specified
      in [java.util.regex.Matcher.replaceAll(java.lang.String)](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html#replaceAll-java.lang.String-)
      method.  
      Example:
      ```yaml
      replacement: 'XX$1'
      ```

##### StationIdPopulator

Set the numeric station id matching station ICAO code, and reject the message if such ICAO code cannot be found in the
database stations table.

[StationIdPopulator](src/main/java/fi/fmi/avi/archiver/message/populator/StationIdPopulator.java) is implicitly added in
the end of the message populator execution chain, and it cannot be omitted nor configured in the middle of the execution
chain.

* **name:** ~
* **config:** ~

#### Conditional message popular activation

Message populator configuration may include an _activation condition_, in which case the populator is executed only when
the provided condition is satisfied. An activation condition consists of one or more _activation expressions_, that
consist of [_activation property_](#activation-property) and one or more [_activation operator_ and _activation
operand_](#activation-operator-and-operand) pairs. When multiple activation expressions and/or operator-operand pairs
are specified, they are combined with AND operator, thus all of them must be satisfied to activate the message
populator.

Template for activation condition configuration:

```yaml
activate-on:
  <activation property>:
    <activation operator>: <activation operand>
    ...
  ...
```

Conditional execution is provided
by [ConditionalMessagePopulator](src/main/java/fi/fmi/avi/archiver/message/populator/conditional/ConditionalMessagePopulator.java)
class.

##### Activation property

Activation property is the name of the aviation message data property that is applied as the first operand
of [activation operator](#activation-operator-and-operand).

The following activation properties are declared
in [MessagePopulatorConditionPropertyReaderConfig](src/main/java/fi/fmi/avi/archiver/config/MessagePopulatorConditionPropertyReaderConfig.java):

* `format`: populated message format name. See `format-ids` in [Identifier mappings](#identifier-mappings).
* `productId`: identifier of the product this file belongs to.
* `route`: populated route name. See `route-ids` in [Identifier mappings](#identifier-mappings).
* `station`: populated station ICAO code.
* `type`: populated message type name. See `type-ids` in [Identifier mappings](#identifier-mappings).
* `<heading>-data-designator`: input bulletin heading data designators (TTAAii).
* `<heading>-originator`: input bulletin heading location indicator / originator (CCCC).

The `<heading>` specifies whether to read GTS or COLLECT heading. It may be one of:

* `gts`: GTS heading
* `collect`: collect identifier
* `gts-or-collect`: GTS heading, or if not present, collect identifier
* `collect-or-gts`: collect identifier, or if not present, GTS heading

E.g. `gts-or-collect-data-designator`.

##### Activation operator and operand

In an activation expression, the activation operator is applied to the [activation property](#activation-property)
value (as first operand for the operator) and specified activation operand (as second operand for the operator).
Activation operand type and possible values depend on the activation operator and activation property.

Activation operator may be one of:

* `presence`: test for presence of activation property.  
  Activation operand is one of:
    * `PRESENT`: activation property value must be present. This is the default when `presence` is omitted.  
      For example, omitting `presence` is equivalent to:
      ```yaml
      activate-on:
        type:
          presence: PRESENT
      ```
    * `EMPTY`: activation property value must not be present
    * `OPTIONAL`: activation property value may or may not be present, aka presence condition is always satisfied
* `is`: test whether activation property is equal to activation operand.  
  This is mutually exclusive with `is-any-of`.

  For example, following activation condition is satisfied when message format is IWXXM.
  ```yaml
  activate-on:
    format:
      is: IWXXM
  ```
* `is-any-of`: test whether activation property is equal to any of activation operand list.  
  This is mutually exclusive with `is`.

  For example, following activation condition is satisfied when message type is either METAR or SPECI.
  ```yaml
  activate-on:
    type:
      is-one-of:
        - METAR
        - SPECI
  ```
* `is-not`: test whether activation property is not equal to activation operand.  
  This is mutually exclusive with `is-none-of`.

  For example, following activation condition is satisfied when message format is not IWXXM.
  ```yaml
  activate-on:
    format:
      is-not: IWXXM
  ```
* `is-none-of`: test whether activation property is not equal to any of activation operand list.  
  This is mutually exclusive with `is-not`.

  For example, following activation condition is satisfied when message type is neither METAR nor SPECI.
  ```yaml
  activate-on:
    type:
      is-none-of:
        - METAR
        - SPECI
  ```
* `matches`: test whether activation property matches regular expression provided as activation operand.

  For example, following activation condition is satisfied when originator parsed from GTS bulletin heading, or collect
  identifier when GTS heading is not present, starts with 'XX':
  ```yaml
  activate-on:
    gts-or-collect-originator:
      matches: 'XX[A-Z]{2}'
  ```
* `does-not-match`: test whether activation property does not match regular expression provided as activation operand.

  For example, following activation condition is satisfied when originator parsed from GTS bulletin heading, or collect
  identifier when GTS heading is not present, does not start with 'XX':
  ```yaml
  activate-on:
    gts-or-collect-originator:
      does-not-match: 'XX[A-Z]{2}'
  ```

Activation operators are provided
by [GeneralPropertyPredicate](src/main/java/fi/fmi/avi/archiver/message/populator/conditional/GeneralPropertyPredicate.java)
class.

### Identifier mappings

Internally this application uses static values to represent message type and format. These internal values differ from
the identifier of equivalent database entity, and are not bound to the name in the database. Therefore, internal values
need to be mapped to database values in the application configuration. In addition to message type and format, message
route is a similar property, but it has no internal semantics. However, it is mapped in the configuration for
consistency with other similar properties.

The following mappings must exist under `production-line` application configuration property:

* `route-ids`: Map route name, preferably same as database column `avidb_message_routes.name`, to database
  column `avidb_message_routes.route_id`.
* `format-ids`:
  Map [`GenericAviationWeatherMessage.Format.name()`](https://github.com/fmidev/fmi-avi-messageconverter/blob/fmi-avi-messageconverter-6.0.0/src/main/java/fi/fmi/avi/model/GenericAviationWeatherMessage.java)
  to database column `avidb_message_format.format_id`.
* `type-ids`:
  Map [`MessageType.name()`](https://github.com/fmidev/fmi-avi-messageconverter/blob/fmi-avi-messageconverter-6.0.0/src/main/java/fi/fmi/avi/model/MessageType.java)
  to database column `avidb_message_types.type_id`.

See the provided [application.yml] for an example.

### Spring Boot configuration properties

Many of the properties in [application.yml] configuration file control the behavior of Spring Boot features. Look at
the [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/)
for more information on these. Some of related sections are:

* [Profiles](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/features.html#features.profiles)
* [Logging](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/features.html#features.logging)
* [Data](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/data.html)
    * [Data Properties](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/application-properties.html#appendix.application-properties.data)
    * [Data Migration Properties](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/application-properties.html#appendix.application-properties.data-migration)
* [Graceful Shutdown](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/web.html#web.graceful-shutdown)
    * [Timeout property](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/application-properties.html#application-properties.core.spring.lifecycle.timeout-per-shutdown-phase)
* [Actuator Endpoints](https://docs.spring.io/spring-boot/docs/2.6.3/reference/html/actuator.html#actuator.endpoints)

## License

MIT License. See [LICENSE](LICENSE).


[application.yml]: src/main/resources/application.yml
