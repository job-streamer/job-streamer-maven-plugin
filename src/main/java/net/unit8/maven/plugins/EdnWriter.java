package net.unit8.maven.plugins;

import us.bpsm.edn.printer.Printers;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author kawasima
 */
@Provider
@Priority(Priorities.ENTITY_CODER)
public class EdnWriter implements MessageBodyWriter<Object> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.getType().equals("application")
                && mediaType.getSubtype().equals("edn");
    }

    @Override
    public long getSize(Object application, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(Object application, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {

        entityStream
                .write(Printers.printString(application).getBytes());
        entityStream.flush();

    }
}
