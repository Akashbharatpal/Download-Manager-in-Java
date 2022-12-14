import java.io.*;
import java.net.*;
import java.util.*;

// This class downloads a file from a URL.
public class Download extends Observable implements Runnable{
//    Max size of download buffer.
    private static final int MAX_BUFFER_SIZE = 1024;
//    These are the status names
    public static final String STATUSES[]  = {"Downloading","Paused","Complete","Cancelled","Error"};

//    These are the status codes.
    public static final int DOWNLADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url; //download URL
    private int size; //size of download in bytes
    private int downloaded; //number of bytes downloaded
    private int status; // current status of download

//    Constructor for Download
    public Download(URL url){
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLADING;
//      Begin the download.
        download();
    }
//    get this download's URL
    public String getUrl(){
        return url.toString();
    }

//    Get this downlad's size.
    public int getSize(){
        return size;
    }
//    Get this download's progress.
    public float getProgress(){
        return ((float) downloaded/size)*100;
    }
//    Get this download's status.
    public int getStatus(){
        return status;
    }
//    Pause this download.
    public void pause(){
        status = PAUSED;
        stateChanged();
    }
//    Resume this download.
    public void resume(){
        status = DOWNLADING;
        stateChanged();
        download();
    }
//    Cancel this download.
    public void cancel(){
        status  = CANCELLED;
        stateChanged();
    }
//    Mark this download as having an error.
    private void error(){
        status = ERROR;
        stateChanged();
    }
//    Start or resume downloading.
    private void download(){
        Thread thread = new Thread(this);
        thread.start();
    }
//    Get file name portion of URL.
    private String getFileName(URL url){
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/')+1);
    }
//    Download file.
    public void run(){
        RandomAccessFile file = null;
        InputStream stream = null;

        try{
            // Open connection to URL.
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            Specify what portion of file to download.
            connection.setRequestProperty("Range","bytes="+downloaded+"-");
//            connect to server.
            connection.connect();

//            Make sure response code is in the 200 range.
            if(connection.getResponseCode() / 100 != 2){
                error();
            }
//            Check for vvalid content length.
            int contentLength = connection.getContentLength();
            if(contentLength < 1){
                error();
            }
//            Set the size for this download if it hasn't been already set.
            if(size == -1){
                size = contentLength;
                stateChanged();
            }
//            Open file and seek to the end of it.
            file = new RandomAccessFile(getFileName(url),"rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while(status == DOWNLADING){
//                Size buffer according to how much of the file is left to download.
                byte buffer[];
                if(size - downloaded > MAX_BUFFER_SIZE){
                    buffer = new byte[MAX_BUFFER_SIZE];
                }else {
                    buffer = new byte[size - downloaded];
                }
//                Read from server into buffer.
                int read = stream.read(buffer);
                if(read == -1)
                    break;
//                Write buffer to file.
                file.write(buffer, 0, read);
                downloaded+=read;
                stateChanged();
            }
//            Change status to complete if this point was reached because downloading has finished.
            if(status == DOWNLADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            error();
        } finally {
            //close file
            if(file != null){
                try{
                    file.close();
                } catch (Exception e){}
            }
//            Close connection to server.
            if(stream != null){
                try{
                    stream.close();
                } catch (Exception e){}
            }
        }
    }
//    Notify observers that this download's status has changed.
    private void stateChanged(){
        setChanged();
        notifyObservers();
    }
}