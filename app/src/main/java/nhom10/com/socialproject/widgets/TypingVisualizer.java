package nhom10.com.socialproject.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Random;

public class TypingVisualizer extends View {

    Random random = new Random();

    Paint paint = new Paint();

    private Runnable animateView = new Runnable() {
        @Override
        public void run() {

            //run every 100 ms
            postDelayed(this, 120);

            invalidate();
        }
    };

    public TypingVisualizer(Context context) {
        super(context);
        new TypingVisualizer(context, null);
    }

    public TypingVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);

        //start runnable
        removeCallbacks(animateView);
        post(animateView);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //set paint style, Style.FILL will fill the color, Style.STROKE will stroke the color
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(getDimensionInPixel(5), getHeight() - (40 + random.nextInt((int) (getHeight() / 1.5f) - 30)), getDimensionInPixel(7), getHeight() - 25, paint);
        canvas.drawRect(getDimensionInPixel(15), getHeight() - (40 + random.nextInt((int) (getHeight() / 1.5f) - 30)), getDimensionInPixel(17), getHeight() -25, paint);
        canvas.drawRect(getDimensionInPixel(25), getHeight() - (40 + random.nextInt((int) (getHeight() / 1.5f) - 30)), getDimensionInPixel(27), getHeight() -25, paint);
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    //get all dimensions in dp so that views behaves properly on different screen resolutions
    private int getDimensionInPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            removeCallbacks(animateView);
            post(animateView);
        } else if (visibility == GONE) {
            removeCallbacks(animateView);
        }
    }
}
