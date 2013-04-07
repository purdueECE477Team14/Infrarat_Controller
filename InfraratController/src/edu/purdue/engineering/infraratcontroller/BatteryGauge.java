package edu.purdue.engineering.infraratcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.View;

public class BatteryGauge extends View {
	
	private int xLarge;
	private int yLarge;
	private int wLarge;
	private int hLarge;
	private int colorLarge;
	private ShapeDrawable shapeLarge;
	
	private int xSmall;
	private int xSmallOrig;
	private int ySmall;
	private int wSmall;
	private int wSmallOrig;
	private int hSmall;
	private int colorSmall;
	private ShapeDrawable shapeSmall;
	
	private int xNub;
	private int yNub;
	private int wNub;
	private int hNub;
	private int colorNub;
	private ShapeDrawable shapeNub;
	
	static private int batPercent;
	
	public BatteryGauge(Context context, int posX, int posY, int width, int height, int startPercentage) {
		super(context);
		
		xLarge = posX;
		yLarge = posY;
		wLarge = width;
		hLarge = height;
		colorLarge = Color.WHITE;
		shapeLarge = new ShapeDrawable(new RectShape());
		shapeLarge.getPaint().setColor(colorLarge);
		shapeLarge.setBounds(xLarge, yLarge, xLarge+wLarge, yLarge+hLarge);
		
		xSmall = xLarge;
		xSmallOrig = xSmall;
		ySmall = yLarge;
		wSmall = wLarge;
		wSmallOrig = wSmall;
		hSmall = hLarge;
		colorSmall = Color.GREEN;
		shapeSmall = new ShapeDrawable(new RectShape());
		shapeSmall.getPaint().setColor(colorSmall);
		shapeSmall.setBounds(xSmall, ySmall, xSmall+wSmall, ySmall+hSmall);
		
		xNub = posX-3;
		yNub = posY+hLarge/3;
		wNub = hLarge/3;
		hNub = hLarge/3;
		colorNub = Color.WHITE;
		shapeNub = new ShapeDrawable(new RectShape());
		shapeNub.getPaint().setColor(colorNub);
		shapeNub.setBounds(xNub, yNub, xNub+wNub, yNub+hNub);
		
		updateBattery(startPercentage);
	}
	
	protected void onDraw(Canvas canvas) {
		shapeLarge.draw(canvas);
		shapeNub.draw(canvas);
		shapeSmall.draw(canvas);
		
	}
	
	public synchronized void updateBattery(int percentage)
	{
		batPercent = percentage;
		wSmall = (int)((float)wSmallOrig*(((float)batPercent)/100.0f));
		xSmall = xSmallOrig+(wSmallOrig-wSmall);
		if(percentage <=10)
		{
			colorSmall = Color.RED;
		} else
		{
			colorSmall = Color.GREEN;
		}
		
		shapeSmall.getPaint().setColor(colorSmall);
		shapeSmall.setBounds(xSmall, ySmall, xSmall+wSmall, ySmall+hSmall);
	}
}
