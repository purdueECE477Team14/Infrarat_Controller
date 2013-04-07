package edu.purdue.engineering.infraratcontroller;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.*;
import android.util.Log;
import android.view.View;

public class Pixel extends View{
	static final int NULL_COLOR = Color.argb(255, 200, 200, 200); //Color to display when there is no temp data present
	
	//values of blue for the temperature gradient
	static final int BlueGradient[] = {255,253,251,249,247,245,243,241,239,237,235,233,231,229,227,225,223,221,219,217,215,
		213,211,209,207,205,203,201,199,197,195,193,191,189,187,185,183,181,179,177,175,173,171,169,167,165,163,161,159,157,
		155,153,151,149,147,145,143,141,139,137,135,133,131,129,127,125,123,121,119,117,115,113,111,109,107,105,103,101,99,
		97,95,93,91,89,87,85,83,81,79,77,75,73,71,69,67,65,63,61,59,57,55,53,51,49,47,45,43,41,39,37,35,33,31,29,27,25,23,21,
		19,17,15,13,11,9,7,5,3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	
	//values of green for the temperature gradient
	static final int GreenGradient[] = {0,1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39,41,43,45,47,49,51,53,55,57,59,
		61,63,65,67,69,71,73,75,77,79,81,83,85,87,89,91,93,95,97,99,101,103,105,107,109,111,113,115,117,119,121,123,125,127,129,
		131,133,135,137,139,141,143,145,147,149,151,153,155,157,159,161,163,165,167,169,171,173,175,177,179,181,183,185,187,189,
		191,193,195,197,199,201,203,205,207,209,211,213,215,217,219,221,223,225,227,229,231,233,235,237,239,241,243,245,247,249,
		251,253,255,253,251,249,247,245,243,241,239,237,235,233,231,229,227,225,223,221,219,217,215,213,211,209,207,205,203,201,
		199,197,195,193,191,189,187,185,183,181,179,177,175,173,171,169,167,165,163,161,159,157,155,153,151,149,147,145,143,141,
		139,137,135,133,131,129,127,125,123,121,119,117,115,113,111,109,107,105,103,101,99,97,95,93,91,89,87,85,83,81,79,77,75,73,
		71,69,67,65,63,61,59,57,55,53,51,49,47,45,43,41,39,37,35,33,31,29,27,25,23,21,19,17,15,13,11,9,7,5,3,0};
	
	//values of red for the temperature gradient
	static final int RedGradient[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39,41,43,45,47,49,51,53,55,57,59,
			61,63,65,67,69,71,73,75,77,79,81,83,85,87,89,91,93,95,97,99,101,103,105,107,109,111,113,115,117,119,121,123,125,127,129,
			131,133,135,137,139,141,143,145,147,149,151,153,155,157,159,161,163,165,167,169,171,173,175,177,179,181,183,185,187,189,
			191,193,195,197,199,201,203,205,207,209,211,213,215,217,219,221,223,225,227,229,231,233,235,237,239,241,243,245,247,249,
			251,253,255};
	
	static final int NUMOFGRADIENTS = 255; //max position in the arrays for gradients
	static final int MINTEMP = -50; //min temperature of the IR sensors
	static final int MAXTEMP = 300; //max temperature of the IR sensors
	
	private int x;
	private int y;
	private int height;
	private int width;
	
	private int color;
	
	private ShapeDrawable drawable;
	
	public Pixel(Context context, int posX, int posY, int size)
	{
		super(context);

		x = posX;
		y = posY;
		width = size;
		height = size;
		this.color = NULL_COLOR;

		drawable = new ShapeDrawable(new RectShape());
		drawable.getPaint().setColor(color);
		drawable.setBounds(x, y, x + width, y + height);

	}
	
	public void changePixel(int posX, int posY, int size)
	{
		x = posX;
		y = posY;
		width = size;
		height = size;
		drawable.setBounds(x, y, x + width, y + height);
	}
	
	protected void onDraw(Canvas canvas) {
		drawable.draw(canvas);
	}


	public void setColor(int red, int green, int blue)
	{
		this.color = Color.argb(255, red, green, blue);
		drawable.getPaint().setColor(color);
	}
	
	public synchronized void setGradient(int gradValue)
	{
		this.color = Color.argb(255, RedGradient[gradValue], GreenGradient[gradValue], BlueGradient[gradValue]);
		drawable.getPaint().setColor(color);
	}
	
	public void setGradientFromTemperature(int temperature)
	{
		setGradient((int)(((float)NUMOFGRADIENTS/(float)(MAXTEMP-MINTEMP))*(float)(temperature+50)));
	}
	
	public void setCoords(int x, int y, int size)
	{
		this.x = x;
		this.y = y;
		width = size;
		height = size;
	}
	
	
	public int getBottomShape()
	{
		return y+height;
	}
	
	public int getTopShape()
	{
		return y;
	}
	
	public int getLeftShape()
	{
		return x;
	}
	
	public int getRightShape()
	{
		return x+width;
	}
	
	public int getTopLeftXShape()
	{
		return x;
	}
	
	public int getTopLeftYShape()
	{
		return y;
	}
	
	public int getTopRightXShape()
	{
		return x+width;
	}
	
	public int getTopRightYShape()
	{
		return y;
	}
	
	public int getBottomLeftXShape()
	{
		return x;
	}
	
	public int getBottomLeftYShape()
	{
		return y+height;
	}
	
	public int getBottomRightXShape()
	{
		return x+width;
	}
	
	public int getBottomRightYShape()
	{
		return y+height;
	}
}
