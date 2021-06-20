package com.miaoshaproject.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.beans.Customizer;

/**
 * 当spring容器内没有TomcatEmbeddedServletContainerFactory这个bean时，
 * 会把此bean加载进spring
 *
 * @author yzze
 * @create 2021-06-20 16:04
 */
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        // 使用对应工厂类提供给我们的接口定制化tomcat connetor
        ((TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocolHandler = (Http11NioProtocol)connector.getProtocolHandler();
                // 定制化 keepalivetimeout
                // 设置30秒内没有请求则服务端自动断开keepalive链接
                protocolHandler.setKeepAliveTimeout(30000);
                // 当客户端发送超过10000个请求则自动断开keepalive链接
                protocolHandler.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
