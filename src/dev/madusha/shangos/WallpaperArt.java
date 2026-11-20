package dev.madusha.shangos;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/** Draws what {@link Wallpaper} shows: a picture fitted to the screen under a legibility veil. */
final class WallpaperArt {
    private WallpaperArt() {}

    /**
     * Scales the picture to cover the target (cropping the excess) and adds the veil that keeps the
     * UI's text legible.
     */
    @WorkerThread
    @NonNull
    static Bitmap compose(@NonNull Bitmap source, boolean pale, int width, int height) {
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        float scale =
                Math.max(width / (float) source.getWidth(), height / (float) source.getHeight());
        float w = source.getWidth() * scale, h = source.getHeight() * scale;
        c.drawBitmap(
                source,
                null,
                new RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2),
                paint);
        if (pale) {
            // A white veil behind the greeting and the favourites keeps dark text crisp on a pale
            // picture.
            paint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            width * .75f,
                            0,
                            new int[] {0x8CF6F6F8, 0x33F6F6F8, 0x00F6F6F8},
                            new float[] {0, .5f, 1},
                            Shader.TileMode.CLAMP));
            c.drawRect(0, 0, width, height, paint);
            paint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            new int[] {0x00F2F2F5, 0x00F2F2F5, 0x59F2F2F5},
                            new float[] {0, .55f, 1},
                            Shader.TileMode.CLAMP));
            c.drawRect(0, 0, width, height, paint);
        } else {
            paint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            width * .88f,
                            0,
                            new int[] {0x88000000, 0x28000000, 0x00000000},
                            new float[] {0, .5f, 1},
                            Shader.TileMode.CLAMP));
            c.drawRect(0, 0, width, height, paint);
            paint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            new int[] {0x30000000, 0x06000000, 0x70000000, 0xA8000000},
                            new float[] {0, .3f, .68f, 1},
                            Shader.TileMode.CLAMP));
            c.drawRect(0, 0, width, height, paint);
        }
        return result;
    }
}
