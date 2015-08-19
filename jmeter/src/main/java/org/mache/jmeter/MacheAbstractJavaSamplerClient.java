package org.mache.jmeter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
abstract public class MacheAbstractJavaSamplerClient  extends AbstractJavaSamplerClient implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(MacheAbstractJavaSamplerClient.class);

    static protected Map<String, String> ExtractParameters(JavaSamplerContext context) {
        Map<String, String> mapParams = new ConcurrentHashMap<String, String>();
        for (Iterator<String> it = context.getParameterNamesIterator(); it.hasNext();) {
            String paramName =  it.next();
            String paramValue = context.getParameter(paramName);
            mapParams.put(paramName, paramValue);
        }
        return mapParams;
    }

    static protected SampleResult SetupResultForError(SampleResult result, Exception e) {
        LOG.error("Error occured during jmeter run.", e);

        result.sampleEnd();
        result.setSuccessful(false);
        result.setResponseMessage("Exception: " + e);

        return result;
    }
}
