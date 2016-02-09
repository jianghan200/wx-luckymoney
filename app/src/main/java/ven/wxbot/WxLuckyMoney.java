package ven.wxbot;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 江瀚 Han 15/7/27
 * 微信自动加好友
 */
public class WxLuckyMoney extends AccessibilityService {

    static final String TAG = "WxLuckyMoney";
    static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    static final String HONGBAO_TEXT_KEY = "[微信红包]";

    //在后台重复执行任务，每250毫秒执行一次
    int counter = 0;
    static final int UPDATE_INTERVAL = 250;
    private Timer timer = new Timer();

    //防止重复拆红包
    static long lastOpenTime = System.currentTimeMillis()-3000;
    static boolean isFromDetail = false;
    static boolean isRobotClick = false;

    //如果通知来了，五秒内不断的扫描
    static boolean isNotifictaionCome = false;
    static long lastNotifictaionTime = System.currentTimeMillis();

    //总红包金额与个数
    static float totalSum;
    static int totalNum;



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();



        Log.d(TAG, "事件---->" + event);

        //通知栏事件
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Log.i(TAG, "产生通知状态变化：" + event.getClassName() + "");

            List<CharSequence> texts = event.getText();
            if (!texts.isEmpty()) {
                for (CharSequence t : texts) {
                    String text = String.valueOf(t);
                    if (text.contains(HONGBAO_TEXT_KEY)) {

                        isNotifictaionCome = true;
                        lastNotifictaionTime = System.currentTimeMillis();

                        //打开通知栏
                        openNotification(event);

                        //在小米手机上，打开通知后不会产生窗口状态变化，模拟一个
                        sendWindowEvent();
                        break;
                    }
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.i(TAG+"-window", "产生窗口状态变化：" + event.getClassName() + "");

            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

            if (nodeInfo == null) {
                Log.w(TAG, "rootWindow为空");
                return;
            }

            printNode(nodeInfo,0,0);
            if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {

                parseDetail();

            }else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {

                //点中了红包，下一步就是去拆红包
                isFromDetail = false;
                chaiHongbao();

            }else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {

                openHongBao(event);
            }else{
                isFromDetail = false;
            }

        } else if (eventType == 369) {
            Log.i(TAG, "程序模拟打开窗口：" + event.getClassName() + "");
            openHongBao(event);
        }
    }

    private void sendWindowEvent() {

        Log.i(TAG, "程序模拟打开窗口：" + " 我被执行了");
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (!manager.isEnabled()) {
            return;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(369);
        event.setEventType(369);
        event.setPackageName(WECHAT_PACKAGENAME);
        event.setClassName("com.tencent.mm.ui.LauncherUI");

        manager.sendAccessibilityEvent(event);
    }


    @Override
    public void onInterrupt() {
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "连接抢红包服务", Toast.LENGTH_SHORT).show();

        openHongbaoRepeatedly();
    }

    private void openHongbaoRepeatedly() {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //5秒内会不断的点击
                if (isNotifictaionCome) {
                    if (System.currentTimeMillis() - lastNotifictaionTime < 5000) {
                        Log.d(TAG, "重复执行第" + String.valueOf(++counter) + "次");
                        dianjiHongbao();
                    } else {
                        isNotifictaionCome = false;
                    }
                }
            }
        }, 0, UPDATE_INTERVAL);
    }


    /**
     * 打开通知栏消息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }
        //打开通知栏消息
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {

            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo == null) {
                Log.w(TAG, "rootWindow为空");
                return;
            }

            //如果是机器模拟点击红包，继续退到主界面
            if(isRobotClick){
                AccessibilityNodeInfo backNode = nodeInfo.getChild(0).getChild(0);
                if(backNode!=null){
                    backNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    isRobotClick = false;
                }
            }

            //在聊天界面,去点中红包
            //判断是否需要点击
            if(isFromDetail){
                lastOpenTime = System.currentTimeMillis();
                isFromDetail = false;
            }
            dianjiHongbao();

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void chaiHongbao() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        String[] chaiHongbaoKeys = new String[] {"拆红包","Open","开","開"};

        List<AccessibilityNodeInfo> list  = nodeInfo.findAccessibilityNodeInfosByText("拆红包");

        for(String str : chaiHongbaoKeys){
            list = nodeInfo.findAccessibilityNodeInfosByText(str);
            if(!list.isEmpty()){
                break;
            }
        }

        if(list.isEmpty()){
            //有些版本拆红包页面没有文字
            AccessibilityNodeInfo openNode = nodeInfo.getChild(3);
            if(openNode != null){
                openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

        }else{
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }


    }

    private void parseDetail(){
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        //用来标志 接下来发生窗口状态变化都是来自 红包详情页面
        isFromDetail = true;

        //如果当前页面是机器打开的则自动退出
        if(isRobotClick){

//            //不在详情界面停留
//            AccessibilityNodeInfo backNode = nodeInfo.getChild(1).getChild(0);
//            if(backNode!=null){
//                backNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            }

//            AccessibilityNodeInfo nameNode = nodeInfo.getChild(0).getChild(0).getChild(0);
//            Log.w(TAG, "nameNode"+ nameNode.getText().toString());
//            AccessibilityNodeInfo moneyNode = nodeInfo.getChild(0).getChild(0).getChild(2);
//            Log.w(TAG, "moneyNode"+ moneyNode.getText().toString());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void dianjiHongbao() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        List<AccessibilityNodeInfo> list  = nodeInfo.findAccessibilityNodeInfosByText("红包");

        if (list.isEmpty()) {
            list = nodeInfo.findAccessibilityNodeInfosByText("微信红包");
            for (AccessibilityNodeInfo n : list) {
                Log.i(TAG, "-->微信红包:" + " 领取"+HONGBAO_TEXT_KEY +n);
                lastOpenTime = System.currentTimeMillis();
                isRobotClick = true;
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }else{
            //最新的红包领起
            for (int i = list.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                Log.i(TAG, "-->领取红包:" + " 领取最新红包" + parent);
                if (parent != null) {

                    if(noRepeat()){
                        lastOpenTime = System.currentTimeMillis();
                        isRobotClick = true;
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }

                    break;
                }
            }
        }

//        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");

//        if (list.isEmpty()) {
//            list = nodeInfo.findAccessibilityNodeInfosByText(HONGBAO_TEXT_KEY);
//            for (AccessibilityNodeInfo n : list) {
//                Log.i(TAG, "-->微信红包:" + n);
//
//                if(noRepeat()){
//
//                    lastOpenTime = System.currentTimeMillis();
//                    isRobotClick = true;
//                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                }
//
//                break;
//            }
//        } else {
//            //最新的红包领起
//            for (int i = list.size() - 1; i >= 0; i--) {
//                AccessibilityNodeInfo parent = list.get(i).getParent();
//                Log.i(TAG, "-->领取红包:" + parent);
//                if (parent != null) {
//
//                    if(noRepeat()){
//                        lastOpenTime = System.currentTimeMillis();
//                        isRobotClick = true;
//                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    }
//
//                    break;
//                }
//            }
//        }
    }


    public void printNode(AccessibilityNodeInfo nodeInfo, int deep, int index) {

        int num = nodeInfo.getChildCount();

//        if (deep == 0 && index == 0) {
//            Log.d(TAG, deep + "父节点" + "----> 子节点数量" + num);
//        } else {
//            Log.d(TAG, deep + "" + index + "父节点" + "----> 子节点数量" + num);
//        }


        for (int i = 0; i < num; i++) {
            //获得第 n 个子节点
            AccessibilityNodeInfo ani = nodeInfo.getChild(i);

            String tab = "";
            for(int k =0;k<deep;k++){
                tab = tab+"--";
            }

            if (ani != null && ani.getText() != null) {

                Log.d(TAG, tab + "->" + ani.getText().toString());
                if (ani.getText().toString().equals("Accept")) {
                    //ani.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }

                if (ani.getChildCount() > 0) {
                    printNode(ani, deep + 1, i);
                }
            }
            else if (ani != null) {
              Log.d(TAG, tab + "->" + ani.getText());

                if (ani.getChildCount() > 0) {
                    printNode(ani, deep + 1, i);
                }
            }

        }


    }

    public String getFingerPrint(AccessibilityNodeInfo nodeInfo, int deep, int index) {

        String fp = "";
        int num = nodeInfo.getChildCount();
        fp = fp + num;

        for (int i = 0; i < num; i++) {

            //获得第 n 个子节点
            AccessibilityNodeInfo ani = nodeInfo.getChild(i);
            fp = fp + i;

            if (ani != null && ani.getText() != null) {

                fp = fp + ani.getText().toString();

                fp = fp + ani.getChildCount();
                if (ani.getChildCount() > 0) {
                    getFingerPrint(ani, deep + 1, 0);
                }
            } else if (ani != null) {
                fp = fp + ani.getChildCount();
                if (ani.getChildCount() > 0) {
                    getFingerPrint(ani, deep + 1, 0);
                }
            }

        }

        return MD5.getMD5(fp);
    }

    public String getAppName(AccessibilityNodeInfo nodeInfo){

        AccessibilityNodeInfo appname = nodeInfo.getChild(0).getChild(0).getChild(0);
        if(appname != null){
            if(appname.getText() == null){
                Log.d(TAG, "我不在微信主界面");
                return null;
            }else if(appname.getText().toString().contains("WeChat")){
                Log.d(TAG, "我在微信主界面");
                return "WeChat";
            }
        }
        return null;
    }

    public String getFriendName(AccessibilityNodeInfo nodeInfo){

        AccessibilityNodeInfo appname = nodeInfo.getChild(0).getChild(1);
        if(appname != null){
            if(appname.getText() == null){
                Log.d(TAG, "我不在聊天界面");
                return null;
            }else{
                Log.d(TAG, "我在聊天界面");
                return appname.getText().toString();
            }
        }
        return null;
    }


    public boolean noRepeat(){

        if(System.currentTimeMillis()-lastOpenTime>3000){
            return true;
        }else{
            return false;
        }

//        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
//
//        String friendName = getFriendName(nodeInfo);
//
//        if(friendName == null){
//            Log.i(TAG,"defualt 不在聊天界面");
//            friendName = "default";
//        }
//
//        String fp = getFingerPrint(nodeInfo, 0, 0);
//        Log.i(TAG,fp);
//
//        String oldFp = (String)SPUtils.get(A.context,friendName,"old");
//        if(oldFp.equals(fp)){
//            Log.i(TAG,"已经领取过了");
//            return false;
//        }
//
//        SPUtils.put(A.context, friendName, fp);
//        return true;
    }


}
