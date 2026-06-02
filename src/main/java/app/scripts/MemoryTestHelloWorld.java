package app.scripts;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * HelloWorld

Task Manager → 1.030 MB
Eclipse Memory Analyzer → 425,6 MB

C:\Program Files\Java\jdk-25\bin>jcmd 21184 VM.native_memory summary
21184:
Native Memory Tracking:
(Omitting categories weighting less than 1KB)
Total: reserved=5716805KB, committed=1244161KB
       malloc: 24185KB #50890, peak=67775KB #51520
       mmap:   reserved=5692620KB, committed=1219976KB
-                 Java Heap (reserved=4167680KB, committed=1132544KB)
                            (mmap: reserved=4167680KB, committed=1132544KB, at peak)
-                     Class (reserved=1048782KB, committed=1166KB)
                            (classes #2951)
                            (  instance classes #2638, array classes #313)
                            (malloc=206KB tag=Class #4489) (peak=206KB #4488)
                            (mmap: reserved=1048576KB, committed=960KB, at peak)
                            (  Metadata:   )
                            (    reserved=65536KB, committed=7808KB)
                            (    used=7722KB)
                            (    waste=86KB =1.10%)
                            (  Class space:)
                            (    reserved=1048576KB, committed=960KB)
                            (    used=887KB)
                            (    waste=73KB =7.61%)
-                    Thread (reserved=31834KB, committed=1522KB)
                            (threads #31)
                            (stack: reserved=31744KB, committed=1432KB, peak=1432KB)
                            (malloc=56KB tag=Thread #190) (peak=68KB #203)
                            (arena=34KB #58) (peak=290KB #58)
-                      Code (reserved=249221KB, committed=9025KB)
                            (malloc=1476KB tag=Code #9359) (at peak)
                            (mmap: reserved=247744KB, committed=7548KB, at peak)
                            (arena=1KB #1) (peak=34KB #2)
-                        GC (reserved=132231KB, committed=72815KB)
                            (malloc=17351KB tag=GC #3875) (at peak)
                            (mmap: reserved=114880KB, committed=55464KB, at peak)
                            (arena=0KB #0) (peak=8KB #8)
-                 GCCardSet (reserved=7KB, committed=7KB)
                            (malloc=7KB tag=GCCardSet #64) (peak=7KB #65)
-                  Compiler (reserved=253KB, committed=253KB)
                            (malloc=57KB tag=Compiler #110) (peak=73KB #121)
                            (arena=196KB #6) (peak=34412KB #21)
-                  Internal (reserved=1314KB, committed=1314KB)
                            (malloc=1246KB tag=Internal #3264) (at peak)
                            (mmap: reserved=68KB, committed=68KB, at peak)
-                     Other (reserved=16KB, committed=16KB)
                            (malloc=16KB tag=Other #6) (at peak)
-                    Symbol (reserved=2123KB, committed=2123KB)
                            (malloc=1763KB tag=Symbol #22714) (at peak)
                            (arena=360KB #1) (at peak)
-    Native Memory Tracking (reserved=994KB, committed=994KB)
                            (malloc=99KB tag=Native Memory Tracking #1792) (peak=100KB #1797)
                            (tracking overhead=895KB)
-        Shared class space (reserved=16384KB, committed=14144KB, readonly=0KB)
                            (mmap: reserved=16384KB, committed=14144KB, peak=14400KB)
-               Arena Chunk (reserved=2KB, committed=2KB)
                            (malloc=2KB tag=Arena Chunk #71) (peak=46135KB #874)
-                   Tracing (reserved=8KB, committed=8KB)
                            (malloc=8KB tag=Tracing #40) (peak=9KB #42)
-                   Logging (reserved=0KB, committed=0KB)
                            (malloc=0KB tag=Logging) (peak=1KB #1)
-                    Module (reserved=202KB, committed=202KB)
                            (malloc=202KB tag=Module #2535) (at peak)
-                 Safepoint (reserved=8KB, committed=8KB)
                            (mmap: reserved=8KB, committed=8KB, at peak)
-           Synchronization (reserved=148KB, committed=148KB)
                            (malloc=148KB tag=Synchronization #2327) (at peak)
-            Serviceability (reserved=30KB, committed=30KB)
                            (malloc=30KB tag=Serviceability #18) (peak=30KB #16)
-                 Metaspace (reserved=65567KB, committed=7839KB)
                            (malloc=31KB tag=Metaspace #15) (at peak)
                            (mmap: reserved=65536KB, committed=7808KB, at peak)
-      String Deduplication (reserved=1KB, committed=1KB)
                            (malloc=1KB tag=String Deduplication #8) (at peak)
-           Object Monitors (reserved=1KB, committed=1KB)
                            (malloc=1KB tag=Object Monitors #5) (at peak)
 */

public class MemoryTestHelloWorld extends Application {
	
	private static Image img;
	
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws InterruptedException {
    	
    	generateReportAndStartGarbageCollection("===== Before loading the image =====", false);
    	
    	img = new Image(new File("C:/Users/Markgraf/OneDrive/ThosSuite/data/maps/png/Hannover 500.jpg").toURI().toString());
    	
    	generateReportAndStartGarbageCollection("===== After loading the image =====", false);
    	System.gc();
    	Thread.sleep(1000);
    	System.gc();
    	generateReportAndStartGarbageCollection("===== After triggering GarbageCollection =====", false);
    	
    	Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(() -> generateReportAndStartGarbageCollection("Automatisiert", true), 10, 5, TimeUnit.SECONDS);
    	
        primaryStage.setTitle("Hello World!");
        StackPane root = new StackPane();
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
    
    @Override
    public void stop() throws Exception {
    	generateReportAndStartGarbageCollection("===== After closing the window =====", false);
    	Thread.sleep(1000);
    	generateReportAndStartGarbageCollection("===== After closing the window 2 =====", false);
    	System.out.println();
    	System.out.println("Imagebreite: " + img.getWidth());
    	System.exit(0);
    }
    
    public static String formatSize(long bytes) {
        if (bytes < 1_000) {
            return bytes + " B";
        } else if (bytes < 1_000_000) {
            return (bytes / 1_000) + " KB";
        } else if (bytes < 1_000_000_000) {
            return (bytes / 1_000_000) + " MB";
        } else {
            double gb = bytes / 1_000_000_000.0;
            return String.format("%.1f GB", gb);
        }
    }
    
    public static void generateReportAndStartGarbageCollection (String name, boolean garbageCollection) {
    	if (garbageCollection)
    		System.gc();
    	System.out.println();
    	System.out.println(name);
    	System.out.println(img == null ? "Image ist null" : "Breite des Bildes: " + img.getWidth());
    	System.out.println(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    	MemoryMXBean mxb = ManagementFactory.getMemoryMXBean();
    	MemoryUsage muHeap = mxb.getHeapMemoryUsage();
    	MemoryUsage muNonHeap = mxb.getNonHeapMemoryUsage();
    	System.out.println("Heap");
    	System.out.println("Init: " + formatSize(muHeap.getInit()));
    	System.out.println("Used: " + formatSize(muHeap.getUsed()));
    	System.out.println("Commited: " + formatSize(muHeap.getCommitted()));
    	System.out.println("Max: " + formatSize(muHeap.getMax()));
    	System.out.println("Non Heap");
    	System.out.println("Init: " + formatSize(muNonHeap.getInit()));
    	System.out.println("Used: " + formatSize(muNonHeap.getUsed()));
    	System.out.println("Commited: " + formatSize(muNonHeap.getCommitted()));
    	System.out.println("Max: " + formatSize(muNonHeap.getMax()));
    }
    
}