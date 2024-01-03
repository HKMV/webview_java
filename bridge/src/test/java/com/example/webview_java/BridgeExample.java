package com.example.webview_java;

import java.awt.Toolkit;

import co.casterlabs.commons.async.AsyncTask;
import dev.webview.webview_java.Webview;
import dev.webview.webview_java.bridge.JavascriptFunction;
import dev.webview.webview_java.bridge.JavascriptObject;
import dev.webview.webview_java.bridge.JavascriptValue;
import dev.webview.webview_java.bridge.WebviewBridge;

public class BridgeExample {

    public static void main(String[] args) {
        Webview wv = new Webview(true); // Can optionally be created with an AWT component to be painted on.
        WebviewBridge bridge = new WebviewBridge(wv);

        bridge.defineObject("Test", new TestObject());

        wv.setTitle("My Webview App");
        wv.setSize(800, 600);
        wv.setHTML(
            "<!DOCTYPE html>"
                + "<html>"
                + "<p>Nano Time: <span id='nano-time'></span></p>"
                + "<button onclick='Test.ringBell();'>Ring Bell</button>"
                + "<script>"
                + "Test.__stores.svelte('nanoTime')"
                + ".subscribe((time) => {"
                + "document.querySelector('#nano-time').innerText = time;"
                + "});"
                + "</script>"
                + "</html>"
        );

        wv.run(); // Run the webview event loop, the webview is fully disposed when this returns.
        wv.close(); // Free any resources allocated.
    }

    public static class TestObject extends JavascriptObject {

        @JavascriptValue(allowSet = false, watchForMutate = true)
        public long nanoTime = -1;
        {
            AsyncTask.create(() -> {
                try {
                    while (true) {
                        this.nanoTime = System.nanoTime();
                        Thread.sleep(100);
                    }
                } catch (InterruptedException ignored) {}
            });
        }

        @JavascriptFunction
        public void ringBell() {
            Toolkit.getDefaultToolkit().beep();
        }

    }

}
