package edu.purdue.engineering.infraratcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.*;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class AnalogControl extends View {

	
	private int xCircIn;
	private int yCircIn;
	private int widthCircIn;
	private int heightCircIn;
	private int colorCircIn;
	private ShapeDrawable circInDrawable;
	
	private int xCircOut;
	private int yCircOut;
	private int widthCircOut;
	private int heightCircOut;
	private int colorCircOut;
	private ShapeDrawable circOutDrawable;
	
	private int xStick;
	private int yStick;
	private int xStickOrig;
	private int yStickOrig;
	private int widthStick;
	private int heightStick;
	private int colorStick;
	private ShapeDrawable stickDrawable;
	
	private int activePointerId;
	private static final int INVALID_POINTER_ID = -1;
	
	private boolean isEnabled;
	
	private String name;
	
	public AnalogControl(Context context, int posX, int posY, int size, String name) {
		super(context);
		this.name = name;
		xStick = posX+size/3;
		yStick = posY+size/3;
		xStickOrig = xStick;
		yStickOrig = yStick;
		widthStick = size/3;
		heightStick = size/3;
		colorStick = Color.BLACK;
		stickDrawable = new ShapeDrawable(new OvalShape());
		stickDrawable.getPaint().setColor(colorStick);
		stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
		
		xCircIn = posX+3;
		yCircIn = posY+3;
		widthCircIn = size-6;
		heightCircIn = size-6;
		colorCircIn = Color.WHITE;
		circInDrawable = new ShapeDrawable(new OvalShape());
		circInDrawable.getPaint().setColor(colorCircIn);
		circInDrawable.setBounds(xCircIn, yCircIn, xCircIn + widthCircIn, yCircIn + heightCircIn);
		
		xCircOut = posX;
		yCircOut = posY;
		widthCircOut = size;
		heightCircOut = size;
		colorCircOut = Color.BLACK;
		circOutDrawable = new ShapeDrawable(new OvalShape());
		circOutDrawable.getPaint().setColor(colorCircOut);
		circOutDrawable.setBounds(xCircOut, yCircOut, xCircOut + widthCircOut, yCircOut + heightCircOut);
		
		activePointerId = INVALID_POINTER_ID;
		isEnabled = false;
	}
	
	public void changeAnalog(int posX, int posY, int size)
	{
		xStick = posX+size/3;
		yStick = posY+size/3;
		xStickOrig = xStick;
		yStickOrig = yStick;
		widthStick = size/3;
		heightStick = size/3;
		stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
		
		xCircIn = posX+3;
		yCircIn = posY+3;
		widthCircIn = size-6;
		heightCircIn = size-6;
		circInDrawable.setBounds(xCircIn, yCircIn, xCircIn + widthCircIn, yCircIn + heightCircIn);
		
		xCircOut = posX;
		yCircOut = posY;
		widthCircOut = size;
		heightCircOut = size;
		circOutDrawable.setBounds(xCircOut, yCircOut, xCircOut + widthCircOut, yCircOut + heightCircOut);
	}

	protected void onDraw(Canvas canvas) {
		circOutDrawable.draw(canvas);
		circInDrawable.draw(canvas);
		stickDrawable.draw(canvas);
	}
	
	private boolean isInBounds(int x, int y)
	{
		int centerX = xCircOut+widthCircOut/2;
		int centerY = yCircOut+heightCircOut/2;
		int radius = widthCircOut/2;
		int distFromCenter = Math.abs((int)Math.sqrt(Math.pow(centerX-x,2)+Math.pow(centerY-y,2)));
		if(distFromCenter <= radius)
		{
			return true;
		}
		return false;
	}
	
	private void moveToEdge(int x, int y)
	{
		int centerX = xCircOut+widthCircOut/2;
		int centerY = yCircOut+heightCircOut/2;
		int radius = widthCircOut/2;
		
		double angle = Math.atan2(centerY-y, centerX-x);

		xStick = xStickOrig-(int)(Math.cos(angle)*(double)radius);
		yStick = yStickOrig-(int)(Math.sin(angle)*(double)radius);
	}
	
	public boolean onTouch (MotionEvent event)
	{
		final int action = event.getAction();
		int pointerIndex;
		int pointerId;
		int x;
		int y;
		switch(action & MotionEvent.ACTION_MASK)
		{
		case MotionEvent.ACTION_DOWN :
			x = (int)event.getX();
	        y = (int)event.getY();
	        
	        
	        if(isInBounds(x, y))
	        {
	        	xStick = x-widthStick/2;
				yStick = y-heightStick/2;
				stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
				this.invalidate();
	        	
	        	activePointerId = event.getPointerId((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> 
					MotionEvent.ACTION_POINTER_INDEX_SHIFT);
	        	return true;
	        }
			break;
		case MotionEvent.ACTION_POINTER_DOWN :
			if(activePointerId == INVALID_POINTER_ID)
			{
				pointerId = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> 
					MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				pointerIndex = event.findPointerIndex(pointerId);
				x = (int)event.getX(pointerIndex);
		        y = (int)event.getY(pointerIndex);
		        
		        if(isInBounds(x, y))
		        {

		        	xStick = x-widthStick/2;
					yStick = y-heightStick/2;
					stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
					this.invalidate();
		        	
		        	activePointerId = event.getPointerId((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> 
		        		MotionEvent.ACTION_POINTER_INDEX_SHIFT);
		        	return true;
		        }
				break;
			}
			break;
		case MotionEvent.ACTION_MOVE :
			pointerIndex = event.findPointerIndex(activePointerId);
			if(pointerIndex == -1) break;
	        x = (int)event.getX(pointerIndex);
	        y = (int)event.getY(pointerIndex);

	        
	        if(isInBounds(x, y))
	        {
	        	xStick = x-widthStick/2;
	        	yStick = y-heightStick/2;
	        	stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
	        	this.invalidate();
	        }
	        else
	        {
	        	moveToEdge(x,y);
	        	stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
	        	this.invalidate();
	        }
	        return true;
		case MotionEvent.ACTION_POINTER_UP :
		case MotionEvent.ACTION_UP :
		case MotionEvent.ACTION_CANCEL :
			pointerIndex = event.getActionIndex();
			pointerId = event.getPointerId((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
			
			if (pointerId == activePointerId) {
				xStick = xStickOrig;
				yStick = yStickOrig;
				stickDrawable.setBounds(xStick, yStick, xStick + widthStick, yStick + heightStick);
				this.invalidate();
				activePointerId = INVALID_POINTER_ID;
				return true;
	        }
			break;
		}
		return false;
	}
}
