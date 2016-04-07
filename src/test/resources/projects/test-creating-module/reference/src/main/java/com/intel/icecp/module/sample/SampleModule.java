package com.intel.icecp.module.sample;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.node.utils.ChannelUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.modules.ModuleProperty;

import java.net.URI;

/**
 * An example of a module. See {@link Module} for more information. Note that this module may be limited by the ICECP
 * sandbox.
 */
@ModuleProperty(name = "test-module")
public class SampleModule implements Module {
    private static final Logger LOGGER = LogManager.getLogger();

    private Channel<SampleMessage> sampleChannel;
    private Channel<State> stateChannel;

    public static class SampleMessage implements Message {
        public final int a;
        public final long b;

        public SampleMessage(int a, long b) {
            this.a = a;
            this.b = b;
        }
    }

    @Override
    public void run(Node node, Configuration moduleConfiguration, Channel<State> moduleStateChannel, long moduleId) {
        try {
            LOGGER.info("Module started");

            stateChannel = moduleStateChannel;
            URI uri = ChannelUtils.join(node.getDefaultUri(), "sample-module");

            LOGGER.info("Opening channel: " + uri);
            sampleChannel = node.openChannel(uri, SampleMessage.class, Persistence.DEFAULT);
            sampleChannel.subscribe(sampleMessage -> LOGGER.info("Received a sample message: {}", sampleMessage));

            publishState(State.RUNNING);
        } catch (ChannelIOException | ChannelLifetimeException e) {
            LOGGER.error("Error running sample-module", e);
            publishState(State.ERROR);
        }
    }

    @Override
    public void stop(StopReason reason) {
        LOGGER.info("Module stopped: {}", reason);
        try {
            sampleChannel.close();
            publishState(State.STOPPED);
        } catch (ChannelLifetimeException e) {
            LOGGER.error("Failed to close channel: {}", sampleChannel);
        }
    }

    private void publishState(State state) {
        try {
            stateChannel.publish(state);
        } catch (ChannelIOException e) {
            LOGGER.error("Unable to publish {} state", state, e);
        }
    }
}