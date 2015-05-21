package com.segment.analytics;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import java.io.*;
import java.util.NoSuchElementException;

/**
 * Created by gcooney on 5/12/15.
 */
public class QueueFileTest extends AndroidTestCase {



    public void notestParseQueueFile() throws IOException {
        String fileName = "/Users/gcooney/Downloads/q1r1kn4a44_EOFException_number2";
        File file = new File(fileName);
        QueueFile queueFile = new QueueFile(file);
        System.out.println(queueFile.size());
    }

    public void noprintQueueFile() throws IOException {
        String fileName = "/Users/gcooney/Downloads/q1r1kn4a44_EOFException";
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);

        int nextbyte = 0;
        int counter = 0;
        while (nextbyte >= 0) {
            nextbyte = fis.read();

            if (nextbyte >=0) {
                System.out.print(nextbyte);
                counter++;
            }
        }
        System.out.println();
        System.out.println("numBytes: " + counter);
    }


    public void notestQueueDequeue_oneThread() throws IOException {
        QueueFile file = new QueueFile(new File("/usr/local/scbe/queuefile"));


        QueueFile.ElementVisitor elementVisitor = new QueueFile.ElementVisitor() {
            @Override
            public boolean read(InputStream in, int length) throws IOException {
                byte[] readBytes = new byte[length];
                in.read(readBytes);

                return true;
            }
        };

        doModifyQueueFile(file);
    }

    public void testQueueDequeue_fiveThreads() throws IOException, InterruptedException {
        Context context = new RenamingDelegatingContext(getContext(), "test_");
        File folder = context.getDir("test-segment-disk-queue", Context.MODE_PRIVATE);
        File theFile = new File(folder, "test-queue-file");
        final QueueFile file = new QueueFile(theFile);


        QueueFile.ElementVisitor elementVisitor = new QueueFile.ElementVisitor() {
            @Override
            public boolean read(InputStream in, int length) throws IOException {
                byte[] readBytes = new byte[length];
                in.read(readBytes);

                return true;
            }
        };
        Thread[] threads = new Thread[7];
        for (int i = 0; i<threads.length; i++) {
            Runnable toRun = new Runnable() {
                @Override
                public void run() {

                    try {
                        doModifyQueueFile(file);
                    } catch (IOException e) {
                        System.err.println("IOException: "+ e.getMessage());
                        System.err.println(getStackTraceAsString(e));
                    } catch(Throwable t) {
                        System.err.println("Throwable: " + t.getMessage());
                        System.err.println(getStackTraceAsString(t));
                    }
                }
            };
            threads[i] = new Thread(toRun);
            threads[i].start();
        }

        for (int i = 0; i<threads.length; i++) {
            threads[i].join(15 * 60 * 1000);
        }
    }

    private void doModifyQueueFile(QueueFile file) throws IOException {
        for (int i = 0; i < 500; i++) {
            file.add(generateRandomBytes((int)(5000 * Math.random()) + 1));
        }

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            double randChoice = Math.random();
            if (randChoice < 0.951 || file.size() == 0) {
                file.add(generateRandomBytes((int)(5000 * Math.random()) + 1));
            } else {
                int toRemove = (int) (50 * Math.random());
                if (toRemove > file.size()) {
                    toRemove = file.size();
                }
                try {
                    file.remove(toRemove);
                } catch(NoSuchElementException nse) {
                    System.out.println("NoSuchElementException: "+nse.getMessage());
                    System.out.println(getStackTraceAsString(nse));
                }
            }

//            file.forEach(elementVisitor);
        }
    }


    private byte[] generateRandomBytes(int length) {
        byte[] myBytes = new byte[length];
        for (int i=0; i<myBytes.length; i++) {
            myBytes[i] = (byte)  (Math.random() * 256);
        }

        return myBytes;
    }

    private static String getStackTraceAsString(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

}