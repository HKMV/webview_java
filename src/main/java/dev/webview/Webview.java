package dev.webview;

import static dev.webview.WebviewNative.NULL_PTR;
import static dev.webview.WebviewNative.WV_HINT_FIXED;
import static dev.webview.WebviewNative.WV_HINT_MAX;
import static dev.webview.WebviewNative.WV_HINT_MIN;
import static dev.webview.WebviewNative.WV_HINT_NONE;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Closeable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;

import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import dev.webview.WebviewNative.BindCallback;
import lombok.NonNull;

public class Webview implements Closeable, Runnable {
    private static final WebviewNative N;

    static {
        WebviewNative.runSetup();
        N = Native.load("webview", WebviewNative.class);
    }

    @Deprecated
    public long $pointer;

    public static Component createAWT(boolean debug, @NonNull Consumer<Webview> onCreate) {
        return new Canvas() {
            private static final long serialVersionUID = 5199512256429931156L;

            private boolean initialized = false;

            private Webview webview;
            private Dimension lastSize = null;

            @Override
            public void paint(Graphics g) {
                Dimension size = this.getSize();

                if (!size.equals(this.lastSize)) {
                    this.lastSize = size;

                    if (this.webview != null) {
                        this.updateSize();
                    }
                }

                if (!this.initialized) {
                    this.initialized = true;

                    new Thread(() -> {
                        this.webview = new Webview(debug, this);

                        this.updateSize();

                        onCreate.accept(this.webview);
                    }).start();
                }
            }

            private void updateSize() {
                int width = this.lastSize.width;
                int height = this.lastSize.height;

                // There is a random margin on Windows that isn't visible, so we must
                // compensate.
                // TODO figure out why this is caused.
                if (Platform.osDistribution == OSDistribution.WINDOWS_NT) {
                    width -= 16;
                    height -= 39;
                }

                this.webview.setFixedSize(width, height);
            }

        };
    }

    /**
     * Creates a new Webview.
     * 
     * @param debug Enables devtools/inspect element if true.
     */
    public Webview(boolean debug) {
        this(debug, NULL_PTR);
    }

    /**
     * Creates a new Webview.
     * 
     * @param debug  Enables devtools/inspect element if true.
     * 
     * @param target The target awt component, such as a {@link java.awt.JFrame} or
     *               {@link java.awt.Canvas}
     */
    public Webview(boolean debug, @NonNull Component target) {
        this(debug, new PointerByReference(Native.getComponentPointer(target)));
    }

    /**
     * @deprecated Use this if you absolutely do know what you're doing.
     */
    @Deprecated
    public Webview(boolean debug, @Nullable PointerByReference windowPointer) {
        $pointer = N.webview_create(debug, windowPointer);

        this.loadURL(null);
        this.setSize(800, 600);
    }

    public long getNativeWindowPointer() {
        return N.webview_get_window($pointer);
    }

    public void loadURL(@Nullable String url) {
        if (url == null) {
            url = "about:blank";
        }

        N.webview_navigate($pointer, url);
    }

    public void setTitle(@NonNull String title) {
        N.webview_set_title($pointer, title);
    }

    public void setMinSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_MIN);
    }

    public void setMaxSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_MAX);
    }

    public void setSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_NONE);
    }

    public void setFixedSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_FIXED);
    }

    public void setInitScript(@NonNull String script) {
        N.webview_init($pointer, script);
    }

    public void eval(@NonNull String script) {
        N.webview_eval($pointer, script);
    }

    public void bind(@NonNull String name, @NonNull Function<JsonArray, JsonElement> handler) {
        N.webview_bind($pointer, name, new BindCallback() {
            @Override
            public void callback(long seq, String req, long arg) {
                try {
                    JsonArray arguments = Rson.DEFAULT.fromJson(req, JsonArray.class);

                    try {
                        @Nullable
                        JsonElement result = handler.apply(arguments);

                        N.webview_return($pointer, seq, false, Rson.DEFAULT.toJsonString(result));
                    } catch (Exception e) {
                        N.webview_return($pointer, seq, true, e.getMessage());
                    }
                } catch (JsonParseException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }

    public void unbind(@NonNull String name) {
        N.webview_unbind($pointer, name);
    }

    public void dispatch(@NonNull Runnable handler) {
        N.webview_dispatch($pointer, ($pointer, arg) -> {
            handler.run();
        }, 0);
    }

    @Override
    public void run() {
        N.webview_run($pointer);
        N.webview_destroy($pointer);
    }

    @Override
    public void close() {
        N.webview_terminate($pointer);
    }

}
