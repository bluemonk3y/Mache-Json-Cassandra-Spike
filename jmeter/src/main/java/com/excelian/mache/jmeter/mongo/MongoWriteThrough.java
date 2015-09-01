package com.excelian.mache.jmeter.mongo;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.Map;

/**
 * Mongo JMeter write through test.
 */
public class MongoWriteThrough extends MacheAbstractMongoSamplerClient {
    private static final long serialVersionUID = 3550175542777320608L;

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        Map<String, String> mapParams = extractParameters(context);
        final SampleResult result = new SampleResult();

        result.sampleStart();

        try {

            MongoTestEntity e = initMongoEntity(mapParams);
            cache1.put(e.pkString, e);

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Put value (" + e.description + ") from Cache");
        } catch (Exception e1) {
            return super.setupResultForError(result, e1);
        }

        return result;
    }
}
