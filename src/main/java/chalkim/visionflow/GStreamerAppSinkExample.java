package chalkim.visionflow;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;

public class GStreamerAppSinkExample {

  public static void main(String[] args) {
    // Initialize GStreamer
    Gst.init("GStreamerAppSinkExample");

    // Create a pipeline
    Pipeline pipeline = new Pipeline();

    // Create elements
    Element source = ElementFactory.make("videotestsrc", "source");
    Element convert = ElementFactory.make("videoconvert", "convert");
    AppSink appSink = (AppSink) ElementFactory.make("appsink", "mysink");

    // Configure appsink
    appSink.set("emit-signals", true);
    appSink.connect(new AppSink.NEW_SAMPLE() {
      public FlowReturn newSample(AppSink elem) {
        Sample sample = elem.pullSample();
        Buffer buffer = sample.getBuffer();
        // Process the buffer (e.g., save to file, display, etc.)
        System.out.println("New sample received");
        sample.dispose();
        return FlowReturn.OK;
      }
    });

    // Add elements to the pipeline
    pipeline.addMany(source, convert, appSink);

    // Link elements
    Element.linkMany(source, convert, appSink);

    // Start the pipeline
    pipeline.play();

    // Run the main loop
    Gst.main();

    // Stop the pipeline
    pipeline.stop();
  }
}