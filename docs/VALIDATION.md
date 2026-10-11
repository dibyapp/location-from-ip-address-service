# Validation

Run `./mvnw test` on Linux/macOS or `.\mvnw.cmd test` on Windows with JDK 17.

Ten tests cover Spring context startup, JSON coordinates, missing query parameters, expected HTTP
statuses, HTML page rendering, strict literal IPv4/IPv6 parsing, missing database configuration,
reader reuse/closure, missing coordinates, and sanitized errors. Service tests mock the MaxMind reader;
documentation-reserved IP addresses are used. No licensed database, account, secret, DNS lookup, or
external geolocation request is needed. Local JDK 17 tests and wrapper-based JAR packaging passed. A temporary loopback-only JAR run also verified the page (200), rejected hostname (400), missing query (400), and unconfigured database (503).

GitHub Actions runs the same test suite on Ubuntu, Windows, and macOS. Consult current Actions results.
A pass verifies these behaviors, not accuracy on real public IPs or a production security certification.

An actual City MMDB lookup requires a separately obtained database and is not exercised in CI.
The POM still uses legacy Spring Boot 2.7.3 and GeoIP2 2.15.0; this change does not certify or modernize
the dependency stack. The HTML lookup page and optional external map link should also be checked
manually in your intended browser before relying on them.

## Provider references

- [GeoLite data and access requirements](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data/)
- [MaxMind IP geolocation accuracy](https://support.maxmind.com/knowledge-base/articles/maxmind-geolocation-accuracy)
- [GeoIP2 Java client](https://github.com/maxmind/GeoIP2-java)
- [Apache Maven wrapper](https://maven.apache.org/tools/wrapper/)
