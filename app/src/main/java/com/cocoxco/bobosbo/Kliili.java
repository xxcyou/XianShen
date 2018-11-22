package com.cocoxco.bobosbo;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.*;
import de.robv.android.xposed.callbacks.*;
import de.robv.android.xposed.XposedBridge.*;
import android.app.*;
import android.content.*;
import android.util.*;
import java.util.*;
import java.io.*;
import android.content.pm.*;
import java.sql.*;
import java.lang.reflect.*;
import android.widget.*;
//com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI
public class Kliili implements IXposedHookLoadPackage
{
	private static Map<Long, Object> msgCacheMap = new HashMap<>();
    private static Object storageInsertClazz;
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p1) throws Throwable
	{
		// TODO: Implement this method
		final String G = "Kliili";
		if (!p1.packageName.equals("com.tencent.mm")) return;
		XposedHelpers.findAndHookMethod(Application.class, "attach", 
			Context.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ClassLoader cl = ((Context)param.args[0]).getClassLoader();
					Class<?> msgInfoClass = null;
					Class<?> XPoo = null;
					try {
						XPoo = cl.loadClass("com.tencent.wcdb.database.SQLiteDatabase");
						msgInfoClass = cl.loadClass("com.tencent.mm.storage.bj");
						XposedHelpers.findAndHookMethod(
						XPoo,
						"updateWithOnConflict",
						String.class,
						ContentValues.class,
						String.class,
						String[].class,
						int.class,
						new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) {
								try {
									if (param.args[0].equals("message")) {
										ContentValues contentValues = ((ContentValues) param.args[1]);
										if (contentValues.getAsInteger("type") == 10000 &&
											!contentValues.getAsString("content").equals("你撤回了一条消息") &&
											!contentValues.getAsString("content").equals("You've recalled a message")
											) {
											long msgId = contentValues.getAsLong("msgId");
											Object msg = msgCacheMap.get(msgId);
											long createTime = XposedHelpers.getLongField(msg, "field_createTime");
											XposedHelpers.setIntField(msg, "field_type", contentValues.getAsInteger("type"));
											String pp=splitData(contentValues.getAsString("content"),"\"","\"");
											Log.i(G,"content "+pp);
											XposedHelpers.setObjectField(msg, "field_content",
																		 "『💬防止撤回💬』⇔"+"『💬"+pp+"💬』");
											XposedHelpers.setLongField(msg, "field_createTime", createTime);
											XposedHelpers.callMethod(storageInsertClazz, "b", msg, false);
											param.setResult(1);
										}
									}
									if (param.args[0].equals("SnsInfo")) {
										ContentValues contentValues = ((ContentValues) param.args[1]);
										int type = contentValues.getAsInteger("type");
										int sourceType = contentValues.getAsInteger("sourceType");
										if ((type == 1 || type == 2 || type == 3 || type == 15) && sourceType == 0) {
											handleMomentDelete(contentValues);
										}
									}
									if (param.args[0].equals("SnsComment")) {
										ContentValues contentValues = ((ContentValues) param.args[1]);
										int type = contentValues.getAsInteger("type");
										int commentflag = contentValues.getAsInteger("commentflag");
										if (type != 1 && commentflag == 1) {
											handleCommentDelete(contentValues);
										}
									}
								} catch (Error | Exception e) {
									Log.e(G, "updateWithOnConflict error", e);
									return;
								}
							}
						});
						XposedHelpers.findAndHookMethod(
						XPoo,
						"delete",
						String.class,
						String.class,
						String[].class,
						new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) {
								try {
									String[] media = {"ImgInfo2", "voiceinfo", "videoinfo2", "WxFileIndex2"};
									if (Arrays.asList(media).contains(param.args[0])) {
										param.setResult(1);
									}

								} catch (Error | Exception e) {
									Log.e(G, "delete error", e);
									return;
								}
							}
						});
						XposedHelpers.findAndHookMethod(File.class, "delete", new XC_MethodHook() {
								@Override
								protected void beforeHookedMethod(MethodHookParam param) {
									try {
										String path = ((File) param.thisObject).getAbsolutePath();
										if ((path.contains("/image2/") || path.contains("/voice2/") || path.contains("/video/")))
											param.setResult(true);
									} catch (Error | Exception e) {
										Log.e(G, "file delete error", e);
									}
								}
						});
						XposedBridge.hookAllMethods(msgInfoClass,"b",new XC_MethodHook(){
								@Override
								protected void afterHookedMethod(MethodHookParam param) {
									try {
										storageInsertClazz = param.thisObject;
										Object msg = param.args[0];
										long msgId = XposedHelpers.getLongField(msg, "field_msgId");
										msgCacheMap.put(msgId, msg);
									} catch (Error | Exception e) {
										Log.e(G, "msgInfoClass b error", e);
										return;
									}
								}
						});	
					} catch (Exception e) {
						Log.e(G, "load class error", e);
						return;
					}
				}
		});
		XposedHelpers.findAndHookMethod(android.app.Activity.class, "onStart", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) {
					try {
						if (!(param.thisObject instanceof Activity)) {
							return;
						}
						Activity activity = (Activity) param.thisObject;
						if (activity.getClass().getName().equals("com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI")) {
							Class clazzz = activity.getClass();
							Field field = XposedHelpers.findFirstFieldByExactType(clazzz, Button.class);
							Button button = (Button) field.get(activity);
							if (button != null) {
								button.performClick();
							}
						}

					} catch (Error | Exception e) {
						Log.e(G, "onStart error", e);
					}
				}
		});
		XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", p1.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					List<ApplicationInfo> applicationList = (List) param.getResult();
					List<ApplicationInfo> resultapplicationList = new ArrayList<>();
					for (ApplicationInfo applicationInfo : applicationList) {
						String packageName = applicationInfo.packageName;
						if (isTarget(packageName)) {
							XposedBridge.log("Hid package: " + packageName);
						} else {
							resultapplicationList.add(applicationInfo);
						}
					}
					param.setResult(resultapplicationList);
				}
			});
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", p1.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					List<PackageInfo> packageInfoList = (List) param.getResult();
					List<PackageInfo> resultpackageInfoList = new ArrayList<>();

					for (PackageInfo packageInfo : packageInfoList) {
						String packageName = packageInfo.packageName;
						if (isTarget(packageName)) {
							XposedBridge.log("Hid package: " + packageName);
						} else {
							resultpackageInfoList.add(packageInfo);
						}
					}
					param.setResult(resultpackageInfoList);
				}
			});
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", p1.classLoader, "getPackageInfo", String.class, int.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String packageName = (String) param.args[0];
					if (isTarget(packageName)) {
						param.args[0] = "com.tencent.mm";
						XposedBridge.log("Fake package: " + packageName + " as " + "com.tencent.mm");
					}
				}
			});
		XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", p1.classLoader, "getApplicationInfo", String.class, int.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String packageName = (String) param.args[0];
					if (isTarget(packageName)) {
						param.args[0] = "com.tencent.mm";
						XposedBridge.log("Fake package: " + packageName + " as " + "com.tencent.mm");
					}
				}
			});
        XposedHelpers.findAndHookMethod("android.app.ActivityManager", p1.classLoader, "getRunningServices", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					List<ActivityManager.RunningServiceInfo> serviceInfoList = (List) param.getResult();
					List<ActivityManager.RunningServiceInfo> resultList = new ArrayList<>();

					for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceInfoList) {
						String serviceName = runningServiceInfo.process;
						if (isTarget(serviceName)) {
							XposedBridge.log("Hid service: " + serviceName);
						} else {
							resultList.add(runningServiceInfo);
						}
					}
					param.setResult(resultList);
				}
			});
        XposedHelpers.findAndHookMethod("android.app.ActivityManager", p1.classLoader, "getRunningTasks", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					List<ActivityManager.RunningTaskInfo> serviceInfoList = (List) param.getResult();
					List<ActivityManager.RunningTaskInfo> resultList = new ArrayList<>();

					for (ActivityManager.RunningTaskInfo runningTaskInfo : serviceInfoList) {
						String taskName = runningTaskInfo.baseActivity.flattenToString();
						if (isTarget(taskName)) {
							XposedBridge.log("Hid task: " + taskName);
						} else {
							resultList.add(runningTaskInfo);
						}
					}
					param.setResult(resultList);
				}
			});
		XposedHelpers.findAndHookMethod("android.app.ActivityManager", p1.classLoader, "getRunningAppProcesses", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = (List) param.getResult();
					List<ActivityManager.RunningAppProcessInfo> resultList = new ArrayList<>();

					for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfos) {
						String processName = runningAppProcessInfo.processName;
						if (isTarget(processName)) {
							XposedBridge.log("Hid process: " + processName);
						} else {
							resultList.add(runningAppProcessInfo);
						}
					}
					param.setResult(resultList);
				}
			});
	}
	private boolean isTarget(String name) {
        return name.contains("cocoxco") || name.contains("xposed");
    }
	public static String splitData(String str, String strStart, String strEnd) {
        String tempStr;
        tempStr = str.substring(str.indexOf(strStart) + 1, str.lastIndexOf(strEnd));
        return tempStr;
	}
	private void handleMomentDelete(ContentValues contentValues) {
        String label = "『防⇆删』↭『主⇆题』";
        contentValues.remove("sourceType");
        contentValues.put("content", notifyInfoDelete(label, contentValues.getAsByteArray("content")));
    }

    private void handleCommentDelete(ContentValues contentValues) {
        String label = "[防⇆删]";
        contentValues.remove("commentflag");
        contentValues.put("curActionBuf", notifyCommentDelete(label, contentValues.getAsByteArray("curActionBuf")));
    }

    private byte[] notifyInfoDelete(String head, byte[] msg) {
        if (head == null || msg == null) {
            return null;
        }

        int start = -1;
        for (int i = 0; i < msg.length; i++) {
            if (msg[i] == 0x2A) {
                start = i + 1;
                break;
            }
        }

        int[] res = decodeMsgSize(start, msg);
        int lenSize = res[0];
        int msgSize = res[1];

        byte[] content = Arrays.copyOfRange(msg, start + lenSize, msg.length);

        if (new String(content).startsWith(head)) {
            return msg;
        }

        byte[] len = encodeMsgSize((head + " ").getBytes().length + msgSize);
        return concatAll(Arrays.copyOfRange(msg, 0, start), len, (head + " ").getBytes(), content);
    }

    private byte[] notifyCommentDelete(String head, byte[] msg) {
        if (head == null || msg == null) {
            return null;
        }

        int nameStart = -1;
        for (int i = 0; i < msg.length; i++) {
            if (msg[i] == 0x22) {
                nameStart = i;
                break;
            }
        }
        int start = nameStart + msg[nameStart + 1] + 13;

        int[] res = decodeMsgSize(start, msg);
        int lenSize = res[0];
        int msgSize = res[1];

        byte[] content = Arrays.copyOfRange(msg, start + lenSize, msg.length);

        if (new String(content).startsWith(head)) {
            return msg;
        }

        byte[] len = encodeMsgSize((head + " ").getBytes().length + msgSize);
        return concatAll(Arrays.copyOfRange(msg, 0, start), len, (head + " ").getBytes(), content);
    }

    private int[] decodeMsgSize(int start, byte[] msg) {
        int lenSize = 1;
        int msgSize = msg[start];
        if (msgSize < 0) {
            lenSize = 2;
            int tmp = msg[start + 1];
            msgSize = (msgSize & 0x7F) + (tmp << 7);
        }
        int[] res = {lenSize, msgSize};
        return res;
    }

    private byte[] encodeMsgSize(int msgSize) {
        if ((msgSize >> 7) > 0) {
            byte[] res = {(byte) (msgSize & 0x7F | 0x80), (byte) (msgSize >> 7)};
            return res;
        } else {
            byte[] res = {(byte) msgSize};
            return res;
        }
    }

    private byte[] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
	
}
