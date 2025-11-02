# Okta Group Provider for Trino

A Trino plugin that provides group membership information by integrating with Okta.

## Features

- Integrates with Okta to retrieve user group memberships
- Compatible with Java 21 and later versions
- Supports domain filtering for groups
- Fully integrated with Trino's group provider SPI

## Requirements

- Java 21 or later
- Gradle 8.0 or later
- Trino 477 or later
- Okta account with API access

## Building

Build the plugin using Gradle:

```bash
./gradlew build
```

The plugin JAR will be generated in `build/libs/`.

## Configuration

The plugin requires the following configuration properties in Trino's `etc/group-provider.properties`:

```properties
group-provider.name=okta
okta.url=https://your-okta-domain.okta.com
okta.api.token=your-okta-api-token
okta.domain=optional-domain-filter
```

### Configuration Properties

- `okta.url` (required): Your Okta organization URL (e.g., `https://dev-123456.okta.com`)
- `okta.api.token` (required): Your Okta API token with permissions to read users and groups
- `okta.domain` (optional): Optional domain filter for groups (e.g., `company.com`)

## Deployment

1. Build the plugin:
   ```bash
   ./gradlew build
   ```

2. Create a plugin directory in your Trino installation:
   ```bash
   mkdir -p $TRINO_HOME/plugin/okta-group-provider
   ```

3. Copy the JAR file and all dependencies:
   ```bash
   cp build/libs/trino-group-provider-okta-*.jar $TRINO_HOME/plugin/okta-group-provider/
   ```

4. Create or update the group provider configuration file:
   ```bash
   echo "group-provider.name=okta" > $TRINO_HOME/etc/group-provider.properties
   echo "okta.url=https://your-okta-domain.okta.com" >> $TRINO_HOME/etc/group-provider.properties
   echo "okta.api.token=your-api-token" >> $TRINO_HOME/etc/group-provider.properties
   ```

5. Restart Trino to load the plugin.

## Java Compatibility

This project is configured to work with Java 21 as the minimum version while maintaining compatibility with later Java versions. The Gradle toolchain is configured to use Java 21 for compilation, ensuring bytecode compatibility across Java 21 and later versions.

To verify compatibility:
- The project compiles with Java 21 toolchain
- The bytecode targets Java 21 (which is forward compatible)
- Runtime compatibility with Java 21, 22, 23, and later is maintained

## Development

### Project Structure

```
.
├── build.gradle              # Gradle build configuration
├── settings.gradle           # Gradle settings
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/datacrump/trino/okta/
│   │   │       ├── OktaGroupProviderPlugin.java
│   │   │       ├── OktaGroupProviderFactory.java
│   │   │       └── OktaGroupProvider.java
│   │   └── resources/
│   │       └── META-INF/services/
│   │           └── io.trino.spi.Plugin
│   └── test/
│       └── java/
└── README.md
```

### Running Tests

```bash
./gradlew test
```

## License

Apache License 2.0




Test locally:
```
export OKTA_DOMAIN=....
export OKTA_CLIENT_ID=...
export OKTA_PRIVATE_KEY_PATH=./key.pem
export OKTA_GROUP_PATTERN=GroupForTrino_(.*)
```


Groups for john@datacrump.com: [admin]
Groups for smith@datacrump.com: [marketing]