# Sitemap generator

Java application that goes through website and generates a list of all valid urls encountered during the search, sends the list (XML) to remote location.

Example of HTTP request:

```
POST /xml-handler-uri HTTP/1.1
Host: www.example.com
Content-Type: text/xml

........... XML content ...........
```

CLI implementation:
```
java -jar sitemapgen.jar {targetLocation} {xmlHandler} [{depth}]
```

Parameter | default | Description
--- | --- | ---
targetLocation | - | Website for analysis
xmlHandler | - | Remote location for sitemap.xml dispatch
depth | 3 (int) | Search depth

Example:

```
java -jar sitemapgen.jar http://example.com http://example.com/xml-handler-uri
```
