import com.ibm.wala.util.io.FileUtil;

import java.io.*;
import java.util.*;

public class main{
    public static void main(String[]args) throws IOException {
        HashSet<String> result = addressArgs(args);
        if(result == null) return;
        StringBuffer stringBuffer = new StringBuffer();
        for(String t : result){
            stringBuffer.append(t+"\n");
        }
        File selectionClass = new File("selection-class.txt");
        File selectionMethod = new File("selection-method.txt");
        if ("-c".equals(args[0])) {
            FileUtil.writeFile(selectionClass, stringBuffer.toString());
        }else{
            FileUtil.writeFile(selectionMethod, stringBuffer.toString());
        }

    }
    public static HashSet<String>addressArgs(String[]args){
        Selector selector = new Selector();
        if ("-c".equals(args[0])) {
            selector.init(true);
        } else if ("-m".equals(args[0])) {
            selector.init(false);
        }
        ArrayList<String> src = new ArrayList<String>();
        ArrayList<String> test = new ArrayList<String>();
        File file = new File(args[1]);
        if (file.exists()) {
            if (null == file.listFiles()) {System.out.println("文件为空！\n");}
            LinkedList<File> list = new LinkedList<File>(Arrays.asList(file.listFiles()));
            File t = null;
            File s = null;
            for (File file1 : list) {
                if (file1.getName().equals("test-classes")) {
                    t = file1;
                }
                if (file1.getName().equals("classes")) {
                    s = file1;
                }
            }
            list = new LinkedList<File>(Arrays.asList(t.listFiles()));
            while (!list.isEmpty()) {
                File[] files = list.removeFirst().listFiles();
                if (null == files) {
                    continue;
                }
                for (File f : files) {
                    if (f.isDirectory()) {
                        list.add(f);
                    } else {
                        test.add(f.getPath());
                    }
                }
            }
            list = new LinkedList<File>(Arrays.asList(s.listFiles()));
            while (!list.isEmpty()) {
                File[] files = list.removeFirst().listFiles();
                if (null == files) {
                    continue;
                }
                for (File f : files) {
                    if (f.isDirectory()) {
                        list.add(f);
                    } else {
                        src.add(f.getPath());
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }

        for (String path : test) {
            selector.AddScope(path);
        }

        HashMap<Node, HashSet<Node>> testGraph = new HashMap<Node, HashSet<Node>>();
        selector.makeCallGraph();
        selector.FindDependency(testGraph);

        for (String path : src) {
            selector.AddScope(path);
        }
        HashMap<Node, HashSet<Node>> graph = new HashMap<Node, HashSet<Node>>();
        selector.makeCallGraph();
        selector.FindDependency(graph);
        HashSet<String> result = selector.Select(args[2],graph,testGraph);

        return result;


    }
}