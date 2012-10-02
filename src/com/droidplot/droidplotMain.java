package com.droidplot;

//TODO: Add intent for measurement and setup only.  
//TODO: Add intent for specifying script file
//TODO: Make sure it exits properly when coming back around to it
//TODO: Try out with octave
//TODO: get icon file
//TODO: release iy

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class droidplotMain extends Activity {

	private ProgressDialog mPd_ring;
	private Toast mToast;
	private Canvas mCanvas;
	private Bitmap mBitmap;
	private int mScreenHeight;
	private int mScreenWidth;
	private int mTextHeight;
	private int mTextWidth;
	private int mX;
	private int mY;
	private int mLinetype;
	private int mLinewidth;
	private ViewSwitcher mSwitcher;
	private String mJustMode = "LEFT";
	private DemoView mDemoview = null;
	private LinearLayout mPlotLayout;
	private boolean mReadFile = false;
	private boolean mBackKeyPressed;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		TextView textView = (TextView)findViewById(R.id.myTextView);
		textView.setText("Droidplot launching via Android Terminal Emulator\n\nIf this fails:\nFor now, users are required to have a recent version of the Android Terminal Emulator installed before installing Droidplot.  Please uninstall Droidplot, confirm you have Android Terminal Emulator installed and up to date and then reinstall Droidplot.");
		mToast = Toast.makeText(this, "For now, users are required to have a recent version of the Android Terminal Emulator installed before installing Droidplot.  Please uninstall Droidplot, confirm you have Android Terminal Emulator installed and up to date and then reinstall Droidplot.", Toast.LENGTH_LONG);
		mPlotLayout = (LinearLayout)findViewById(R.id.plotLayout);
		mSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

		ViewTreeObserver viewTreeObserver = mSwitcher.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
			viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					mScreenHeight = mSwitcher.getHeight();
					mScreenWidth = mSwitcher.getWidth();
					if (mScreenWidth < mScreenHeight) {
						mScreenHeight = mScreenWidth;
					} else {
						mScreenWidth = mScreenHeight;
					}
					// Converts 14 dip into its equivalent px
					Resources r = getResources();
					mTextHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, r.getDisplayMetrics());
					Paint paint = new Paint();
					paint.setStyle(Paint.Style.STROKE);
					paint.setTextSize(mTextHeight);
					paint.setTypeface(Typeface.MONOSPACE);
					mTextWidth = (int) paint.measureText("A");
					mPd_ring = ProgressDialog.show(droidplotMain.this, "Unpacking Droidplot", "This may take a while (several minutes if this is the first time).",true);
					mPd_ring.setCancelable(false);
					Thread t = new Thread() {
						public void run() {
							try {
								unpackAllFiles();
								try {
									BufferedWriter bw = new BufferedWriter(new FileWriter("/data/data/com.droidplot/bin/.gnuplot"));
									bw.write("set term android size " + Integer.toString(mScreenWidth) + "," + Integer.toString(mScreenHeight) + " charsize " + Integer.toString(mTextWidth) + "," + Integer.toString(mTextHeight) + " ticsize " + Integer.toString(mTextWidth) + "," + Integer.toString(mTextWidth) + "\n");
									bw.close();
								} catch (Exception e) {
								}
								try {
									Runtime runtime = Runtime.getRuntime(); 
									Process process;
									process = runtime.exec("chmod 0777 /data/data/com.droidplot/bin/.gnuplot");
									try {
										process.waitFor();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								} catch (IOException e1) {
									e1.printStackTrace();
								}
								kickItOff();
							} catch (Exception e) {
								Log.e("LongToast", "", e);
							}
						}
					};
					t.start();

					mSwitcher.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			});
		} 
		onNewIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		String plotFile = getIntent().getStringExtra("plotfile");
		if (plotFile != null) {
			mReadFile = true;
			processFile(plotFile);
		}

	}

	private void kickItOff() {
		mPd_ring.dismiss();
		// opens a new window and runs "echo 'Hi there!'"
		// application must declare jackpal.androidterm.permission.RUN_SCRIPT in manifest
		Intent i = new Intent("jackpal.androidterm.RUN_SCRIPT");
		i.addCategory(Intent.CATEGORY_DEFAULT);
		i.putExtra("jackpal.androidterm.iInitialCommand", "cd /data/data/com.droidplot/bin; ./gnuplot");
		try {
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			fireLongToast();
			//To do: send them to the market in the future
		} catch (SecurityException e) {
			fireLongToast();
		}

	}

	private void fireLongToast() {
		Thread t = new Thread() {
			public void run() {
				int count = 0;
				try {
					while (count < 5) {
						mToast.show();
						sleep(2500);
						count++;
					}
				} catch (Exception e) {
					Log.e("LongToast", "", e);
				}
			}
		};
		t.start();
	}


	private void unpackAllFiles() {

		Runtime runtime = Runtime.getRuntime(); 
		Process process;
		try {
			process = runtime.exec("chmod 0755 /data/data/com.droidplot");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			process = runtime.exec("mkdir /data/data/com.droidplot/bin");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			process = runtime.exec("chmod 0755 /data/data/com.droidplot/bin");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			process = runtime.exec("mkdir /data/data/com.droidplot/tmp");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			process = runtime.exec("chmod 0777 /data/data/com.droidplot/tmp");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			process = runtime.exec("ln -s /data/data/com.droidplot/lib/lib__bin__gnuplot.so /data/data/com.droidplot/bin/gnuplot");
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{ 
		super.onConfigurationChanged(newConfig);
	}

	private void init() {
		if (mBitmap == null) {
			mBitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_8888);
		}
		if (mCanvas == null) {
			mCanvas = new Canvas(mBitmap);
		}

		if (mDemoview == null) {
			mDemoview = new DemoView(this);
			mDemoview.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			mPlotLayout.addView(mDemoview);
			mPlotLayout.invalidate();
			mSwitcher.invalidate();
		}
	}


	private void graphics() {
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);

		// make the entire canvas white
		paint.setColor(Color.WHITE);
		mCanvas.drawPaint(paint);
	}


	private void linewidth(int width) {
		mLinewidth = width;
	}

	private void linetype(int type) {
		mLinetype = type;
	}

	private void justify_text(String mode) {
		mJustMode = mode;
	}

	private void move (int x, int y) {
		mX = x;
		mY = y;
	}

	private void vector (int x, int y) {
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(mLinewidth);

		if (mLinetype < 0) {
			paint.setColor(Color.BLACK);
		} else if ((mLinetype % 9) == 0) {
			paint.setColor(Color.RED);
		} else if ((mLinetype % 9) == 1) {
			paint.setColor(Color.GREEN);
		} else if ((mLinetype % 9) == 2) {
			paint.setColor(Color.BLUE);
		} else if ((mLinetype % 9) == 3) {
			paint.setColor(Color.MAGENTA);
		} else if ((mLinetype % 9) == 4) {
			paint.setColor(Color.CYAN);
		} else if ((mLinetype % 9) == 5) {
			paint.setColor(Color.YELLOW);
		} else if ((mLinetype % 9) == 6) {
			paint.setColor(Color.BLACK);
		} else if ((mLinetype % 9) == 7) {
			paint.setARGB(1,139,69,19);
		} else if ((mLinetype % 9) == 8) {
			paint.setColor(Color.LTGRAY);
		} else {
			paint.setColor(Color.BLACK);
		}
		mCanvas.drawLine(mX, mScreenHeight-mY , x, mScreenHeight-y, paint);
		mX = x;
		mY = y;
	}

	private void text(int x, int y, String text) {
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(mLinewidth);
		paint.setColor(Color.BLACK);
		paint.setTextSize(mTextHeight);
		paint.setTypeface(Typeface.MONOSPACE);
		if (mJustMode.equals("RIGHT")) {
			paint.setTextAlign(Align.RIGHT);
		} else if (mJustMode.equals("CENTRE")) {
			paint.setTextAlign(Align.CENTER);
		} else {
			paint.setTextAlign(Align.LEFT);
		}
		mCanvas.drawText(text, x, mScreenHeight-y, paint);
	}

	private class DemoView extends View{

		public DemoView(Context context){
			super(context);
		}

		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			canvas.drawBitmap(mBitmap,0,0,null);
		}
	}

	public void processFile(String fileName) {

		try {

			/*	Sets up a file reader to read the file passed on the command
				line one character at a time */
			FileReader input = new FileReader(fileName);

			/* Filter FileReader through a Buffered read to read a line at a
			   time */
			BufferedReader bufRead = new BufferedReader(input);

			String line; 	// String that holds current file line

			// Read first line
			line = bufRead.readLine();

			// Read through file one line at time. Print line # and line
			while (line != null){
				if (line.startsWith("ANDROIDTERM")) {
					line = line.replaceAll("\\r|\\n", "");
					String termCommand[] = line.split(",");
					if (termCommand[1].equals("move")) {
						try {
							move(Integer.parseInt(termCommand[2]), Integer.parseInt(termCommand[3]));
						} catch(NumberFormatException ex) {
							//Toast.makeText(getBaseContext(), "why am I here", Toast.LENGTH_LONG).show();
						}
					} else if (termCommand[1].equals("vector")) {
						try {
							vector(Integer.parseInt(termCommand[2]), Integer.parseInt(termCommand[3]));
						} catch(NumberFormatException ex) {
							//Toast.makeText(getBaseContext(), "why am I here", Toast.LENGTH_LONG).show();
						}
					} else if (termCommand[1].equals("put_text")) {
						text(Integer.parseInt(termCommand[2]), Integer.parseInt(termCommand[3]), termCommand[4]);
					} else if (termCommand[1].equals("linetype")) {
						linetype(Integer.parseInt(termCommand[2]));
					} else if (termCommand[1].equals("linewidth")) {
						linewidth(Integer.parseInt(termCommand[2]));
					} else if (termCommand[1].equals("justify_text")) {
						justify_text(termCommand[2]);
					} else if (termCommand[1].equals("init")) {
						init();
					} else if (termCommand[1].equals("graphics")) {
						init();
						graphics();
					} else if (termCommand[1].equals("text")) {
						mDemoview.invalidate();
						mSwitcher.setDisplayedChild(1);
					}
				}
				line = bufRead.readLine();
			}

			bufRead.close();

		}catch (IOException e){
			// If another exception is generated, print a stack trace
			e.printStackTrace();
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mReadFile) {
				if (mSwitcher.getDisplayedChild() != 1) {
					mSwitcher.showPrevious();
					return true;
				} else {
					mBackKeyPressed = true;
					return true;
				}
			} else if (mSwitcher.getDisplayedChild() != 0) {
			} else {
				return super.onKeyDown(keyCode, event);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (!mBackKeyPressed) {
				/* This key up event might correspond to a key down
                       delivered to another activity -- ignore */
				return false;
			}
			mBackKeyPressed = false;
		}
		moveTaskToBack(true);
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		mBackKeyPressed = false;
	}

}