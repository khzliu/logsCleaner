/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package logscleaner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.dom4j.Document;
import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 *
 * @author Administrator
 */
public class LogsCleaner {

    /**
     * @param args the command line arguments
     */
    Map<String,ArrayList<String>> taskMap;
    String initFile = "taskList.xml";
    ArrayList<String> logsFileList;
    float fileTime = 10;
    float fileSize = 10;
    
    //初始化
    public void initConfig() {
        try{
            testParseXmlData(initFile);
        } catch(Exception e){
            System.out.println("配置文件错误！！！");
        }
        
    }
    
    //处理大文件
    public void dealwithBigFile(String fileAbsoluteName, String charset){
        RandomAccessFile rf = null;
        charset = "UTF-8";
        List<String> contentList = new ArrayList(); //临时存放最后100行数据
        try {
            //获取最后100行内容 并存入到contentList中
            rf = new RandomAccessFile(fileAbsoluteName, "rw");  
            long len = rf.length();
            if(len >= fileSize*1024*1024){
                long start = rf.getFilePointer();  
                long nextend = start + len - 1;  
                String line;
                int cowNum = 0;
                rf.seek(nextend);  
                int c = -1;  
                while (nextend > start && cowNum <= 100) {  
                    c = rf.read();  
                    if (c == '\n' || c == '\r') {  
                        line = rf.readLine();  
                        if (line != null) {  
                            contentList.add(new String(line.getBytes("ISO-8859-1"), charset));
                            cowNum = cowNum + 1;
                        } else {  
                            contentList.add(line);
                            cowNum = cowNum + 1;
                        }  
                        nextend--;  
                    }  
                    nextend--;  
                    rf.seek(nextend);  
                    if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行  
                        // System.out.println(rf.readLine());  
                        System.out.println(new String(rf.readLine().getBytes(  
                                "ISO-8859-1"), charset));  
                    }  
                }
                //清空源文件内容
                rf.setLength(0);
                //负责最后100行内容到源文件
                for(int i = contentList.size();i<0;i--)
                {
                    rf.writeChars(contentList.get(i));
                }
            }  
              
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                if (rf != null)  
                    rf.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }            
        
    }
    
    //处理过期文件
    public boolean dealwithLongerTimesFile(String fileAbsoluteName){ 
        File f = new File(fileAbsoluteName);
        long modify	= f.lastModified();	 //   修改时间 
        Date d = new Date();
        long current = d.getTime();

        if((modify + fileTime*24*60*60*1000) <= current) {
            f.delete();
            return false;
        }      
        return true;
    }
    //获取清理目录下的所有文件
    private void getAllFiles(String workSpace,ArrayList<String> filter) {
        // 建立当前目录中文件的File对象  
        File file = new File(workSpace);  
        // 取得代表目录中所有文件的File对象数组  
        File[] list = file.listFiles();
        logsFileList.clear();
        for (int i = 0; i < list.length; i++) {
            if (!list[i].isDirectory())
            {
                String fileName = list[i].getName();
                if(filter.contains(fileName.substring(fileName.lastIndexOf(".")+1,fileName.length()))){
                    logsFileList.add(list[i].getAbsolutePath());
                }
            }
        }
    }
    
    /**
     * 获取指定xml文档的Document对象,xml文件必须在classpath中可以找到
     *
     * @param xmlFilePath xml文件路径
     * @return Document对象
     */ 
    public static Document parse2Document(String xmlFilePath){
        //获取xml解析器对象
        SAXReader reader = new SAXReader();
        Document doc = null;
        try {
            doc = (Document) reader.read(new File(xmlFilePath));
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return doc;
    }
    
    public void testParseXmlData(String xmlFilePath){      
        //将xml解析为Document对象
        Document doc = parse2Document(xmlFilePath);
        //获取文档的根元素
        Element root  = doc.getRootElement();
        //定义保存xml数据的缓冲字符串
        StringBuffer sb = new StringBuffer();
        //获取要删除文件的最大限制大小
        String theTime = root.element("time").getText();
        fileTime = Float.parseFloat(theTime);
        //获取要删除文件的最晚修改时间
        String theSize = root.element("filesize").getText();
        fileSize = Float.parseFloat(theSize);

        for(Iterator i_action=root.elementIterator();i_action.hasNext();){
            Element e_action = (Element)i_action.next();
            for(Iterator a_action=e_action.attributeIterator();a_action.hasNext();){
                Attribute attribute = (Attribute)a_action.next();
                String filePath = attribute.getValue();
                attribute = (Attribute)a_action.next();
                String fliterKind = attribute.getValue();
                ArrayList<String> fliterKinds = fliter(fliterKind);
                taskMap.put(filePath,fliterKinds);
            }
        }
    }
    
    //解析文件格式名fliter
    public ArrayList<String> fliter(String temp){
        String fileKind[] = temp.split("|");
        ArrayList<String> fileKindList = new ArrayList();
        for(String kind:fileKind){
            fileKindList.add(kind);
        }
        return fileKindList;    
    }
    public static void main(String[] args) {
        // TODO code application logic here
        LogsCleaner mainWork = new LogsCleaner();
        mainWork.initConfig();
        while(true){
            for (Entry<String, ArrayList<String>> entry: mainWork.taskMap.entrySet()) {
                String key = entry.getKey();
                ArrayList<String> value = entry.getValue();
                mainWork.logsFileList.clear();
                mainWork.getAllFiles(key, value);
                for(String tem:mainWork.logsFileList){
                    if(mainWork.dealwithLongerTimesFile(tem))
                        mainWork.dealwithBigFile(tem, "UTF-8");
                }

            }
            try {
                Thread.sleep(1000*24*60*60);//括号里面的5000代表5000毫秒，也就是5秒，可以该成你需要的时间
            } catch (InterruptedException e) {
                    e.printStackTrace();
            }
            }
    }
}
