package com.droidplot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class droidplotMain extends Activity {

	private ProgressDialog mPd_ring;
	private Toast mToast;
	private int mScreenHeight;
	private int mScreenWidth;
	private int mTextHeight = 11;
	private int mTextWidth;
	private ViewSwitcher mSwitcher;
	private boolean mReadFile = false;
	private boolean mBackKeyPressed;
	private boolean mMeasureAndExit;
	private String mScriptFile;
	private int mBufferSize = 1024;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		TextView textView = (TextView)findViewById(R.id.myTextView);
		textView.setText("Droidplot launching via Android Terminal Emulator\n\nIf this fails:\nFor now, users are required to have a recent version of the Android Terminal Emulator installed before installing Droidplot.  Please uninstall Droidplot, confirm you have Android Terminal Emulator installed and up to date and then reinstall Droidplot.");
		mToast = Toast.makeText(this, "For now, users are required to have a recent version of the Android Terminal Emulator installed before installing Droidplot.  Please uninstall Droidplot, confirm you have Android Terminal Emulator installed and up to date and then reinstall Droidplot.", Toast.LENGTH_LONG);
		//mPlotLayout = (LinearLayout)findViewById(R.id.plotLayout);
		mSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

		onNewIntent(getIntent());

		ViewTreeObserver viewTreeObserver = mSwitcher.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {
			viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (mReadFile == false) {
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
										File rcFile = new File("/data/data/com.droidplot/share/gnuplot/4.6/gnuplotrc");
										if (rcFile.exists()) {
											rcFile.delete();
										}
										BufferedWriter bw = new BufferedWriter(new FileWriter("/data/data/com.droidplot/share/gnuplot/4.6/gnuplotrc"));
										bw.write("set term png enhanced font \"/data/data/com.droidplot/share/fonts/LiberationMono-Regular.ttf,11\" size " + Integer.toString(mScreenWidth) + "," + Integer.toString(mScreenHeight));
										bw.close();
									} catch (Exception e) {
									}
									try {
										Runtime runtime = Runtime.getRuntime(); 
										Process process;
										process = runtime.exec("chmod 0777 /data/data/com.droidplot/share/gnuplot/4.6/gnuplotrc");
										try {
											process.waitFor();
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									} catch (IOException e1) {
										e1.printStackTrace();
									}
									mPd_ring.dismiss();
									if (mMeasureAndExit) {
										setResult(0);
										finish();
									} else {
										kickItOff();
									}
								} catch (Exception e) {
									Log.e("LongToast", "", e);
								}
							}
						};
						t.start();
					}

					mSwitcher.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			});
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		mMeasureAndExit = getIntent().hasExtra("measureAndExit");
		mScriptFile = getIntent().getDataString();

		String pngFile = getIntent().getStringExtra("pngfile");
		if (pngFile != null) {
			mReadFile = true;
			processFile(pngFile);
		}

	}

	private void kickItOff() {
		// opens a new window and runs "echo 'Hi there!'"
		// application must declare jackpal.androidterm.permission.RUN_SCRIPT in manifest
		Intent i = new Intent("jackpal.androidterm.RUN_SCRIPT");
		i.addCategory(Intent.CATEGORY_DEFAULT);
		if (mScriptFile == null) { 
			mScriptFile = "";
		}
		//i.putExtra("jackpal.androidterm.iInitialCommand", "export GNUTERM='android size " + Integer.toString(mScreenWidth) + "," + Integer.toString(mScreenHeight) + " charsize " + Integer.toString(mTextWidth) + "," + Integer.toString(mTextHeight) + " ticsize " + Integer.toString(mTextWidth) + "," + Integer.toString(mTextWidth) + "'; /data/data/com.droidplot/bin/gnuplot " + mScriptFile);
		//i.putExtra("jackpal.androidterm.iInitialCommand", "umask 000; export HOME=/data/data/com.droidplot/bin; /data/data/com.droidplot/mylib/ld-linux.so.3 --library-path /data/data/com.droidplot/mylib /data/data/com.droidplot/bin/gnuplot " + mScriptFile);
		i.putExtra("jackpal.androidterm.iInitialCommand", "/data/data/com.droidplot/bin/droidplot " + mScriptFile);
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

		String version;

		try {
			PackageInfo pi = getPackageManager().getPackageInfo("com.droidplot", 0);
			version = pi.versionName;     // this is the line Eclipse complains
		} catch (PackageManager.NameNotFoundException e) {
			// eat error, for testing
			version = "?";
		}

		File versionFile = new File("/data/data/com.droidplot/unzippedFiles/version");
		if (versionFile.exists()==false) {
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
				process = runtime.exec("chmod 0755 /data/data/com.droidplot/lib");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} 

			File unzipList = new File("/data/data/com.droidplot/unzippedFiles/");
			if (unzipList.exists()==false) { 
				unzipList.mkdir();
			}

			File tmpDir = new File("/data/data/com.droidplot/bin/");
			if (tmpDir.exists()==false) { 
				tmpDir.mkdir();
			}
			try {
				process = runtime.exec("rm -f /data/data/com.droidplot/bin/*");
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

			tmpDir = new File("/data/data/com.droidplot/mylib/");
			if (tmpDir.exists()==false) { 
				tmpDir.mkdir();
			}
			try {
				process = runtime.exec("rm -f /data/data/com.droidplot/mylib/*");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				process = runtime.exec("chmod 0777 /data/data/com.droidplot/mylib");
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			tmpDir = new File("/data/data/com.droidplot/tmp/");
			if (tmpDir.exists()==false) { 
				tmpDir.mkdir();
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

			File fileList = new File("/data/data/com.droidplot/lib/"); 
			if (fileList != null) { 
				File[] filenames = fileList.listFiles(); 
				for (File tmpf : filenames) { 
					String fileName = tmpf.getName();
					if (fileName.startsWith("lib__")) {
						String[] splitStr;
						splitStr = fileName.substring(0,fileName.length()-3).split("__");  //drop .so and split
						String newFileName = splitStr[2];  //build up correct filename
						for(int i=3; i < splitStr.length ; i++) {
							newFileName = newFileName + "." + splitStr[i];
						} 
						try {
							process = runtime.exec("ln -s /data/data/com.droidplot/lib/" + fileName + " /data/data/com.droidplot/"+splitStr[1]+"/"+newFileName);
							try {
								process.waitFor();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					} else if (fileName.startsWith("libzip")) {
						unzipFile(fileName);
					}
				}
			}
			try {
				versionFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void unzipFile(String filename) { 
		String unzipLocation = "/data/data/com.droidplot/";
		String filePath = "/data/data/com.droidplot/lib/";

		String fullPath = filePath + filename; 
		Log.d("UnZip", "unzipping " + fullPath + " to " + unzipLocation); 
		try {
			unzip(fullPath, unzipLocation);
		} catch (IOException e) {
			Log.d("UnZip", "Failed");
		} 
	}

	public void unzip(String zipFile, String location) throws IOException {
		int size;
		byte[] buffer = new byte[mBufferSize];

		String[] splitPath = zipFile.split("/");
		String[] splitExtension = splitPath[splitPath.length-1].substring(6).split(".so");
		String[] splitVersion = splitExtension[0].split("_");
		deleteDir(new File(location + "/" + splitVersion[0]));

		try {
			File f = new File(location);
			if(!f.isDirectory()) {
				f.mkdirs();
			}
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), mBufferSize));
			try {
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					String path = location + ze.getName();

					File unzipFile = new File(path);
					if (!unzipFile.getParentFile().exists()) {
						Log.v("Unzip", "create parents " + unzipFile.getName());
						createDir(unzipFile.getParentFile()); 
					}
					if (!unzipFile.exists()) {
						if (ze.isDirectory()) {
							Log.v("Unzip", "found directory " + unzipFile.getName());
							if(!unzipFile.isDirectory()) {
								createDir(unzipFile);
							}
						}	else {	
							Log.v("Unzip", "found file " + unzipFile.getName());
							FileOutputStream out = new FileOutputStream(path, false);
							BufferedOutputStream fout = new BufferedOutputStream(out, mBufferSize);
							try {
								while ( (size = zin.read(buffer, 0, mBufferSize)) != -1 ) {
									fout.write(buffer, 0, size);
								}

								zin.closeEntry();
							}
							finally {
								fout.flush();
								fout.close();
								Log.v("Unzip", "changing permissions " + unzipFile.getName());
								Runtime runtime = Runtime.getRuntime(); 
								Process process;
								try {
									process = runtime.exec("chmod 0777 " + unzipFile.getAbsolutePath());
									try {
										process.waitFor();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}
			}
			finally {
				zin.close();
			}
		}
		catch (Exception e) {
			Log.e("main", "Unzip exception", e);
		}
	}

	public boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	private void createDir(File dir) {
		if (!dir.getParentFile().exists()) { 
			createDir(dir.getParentFile()); 
		}
		if (dir.exists()) { 
			return; 
		} 
		Log.v("Unzip", "Creating dir " + dir.getName()); 
		if (!dir.mkdir()) { 
			throw new RuntimeException("Can not create dir " + dir); 
		}
		Runtime runtime = Runtime.getRuntime(); 
		Process process;
		try {
			process = runtime.exec("chmod 0777 " + dir.getAbsolutePath());
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

	public void processFile(String fileName) {
			
		ImageView myImageView = new ImageView(this);
		myImageView.setImageBitmap(BitmapFactory.decodeFile(fileName));
		myImageView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		//mPlotLayout.addView(myImageView);
		//mPlotLayout.invalidate();
		mSwitcher.addView(myImageView);
		mSwitcher.invalidate();
		mSwitcher.showNext();
		//File f1 = new File(fileName);
		//boolean success = f1.delete();

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
		finish();
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mBackKeyPressed = false;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mBackKeyPressed = false;
		File directory = new File("/data/data/com.droidplot/tmp");

		// Get all files in directory
		if (directory.exists()) {
			File[] files = directory.listFiles();
			for (File file : files)
			{
				// Delete each file
				if (!file.delete())
				{
					// Failed to delete file
					System.out.println("Failed to delete "+file);
				}
			}
		}
		finish();
	}

}