# JVT TCP Proxy

JVT (Java Virtual Thread) TCP Proxy is a lightweight and efficient TCP proxy server written in Java. It uses Java's
virtual threads to handle moderate concurrent connections, making it suitable for proxying TCP connections between
ports.

## Development

Run `RunLocal.java` from your IDE to:

* Start a TCP proxy server on port 8080.
* Start a mock server on port 8081.

You can manually test it by running `curl http://localhost:8080/abc`. This will proxy the request to
`http://localhost:8081/abc` and return the mock server's response.
