import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 *依靠node的后继生成graph，根据相关性完成测试用例选择
 */
public class Selector {
    AnalysisScope scope;
    CHACallGraph cg;    //之后利用CHA算法构建调用图
    public Boolean condition;   //isClass

    public void FindDependency(HashMap<Node, HashSet<Node>>graph){//图
        for(CGNode node : cg){
            if (node.getMethod() instanceof ShrikeBTMethod) {
                // 一般地，本项目中所有和业务逻辑相关的方法都是ShrikeBTMethod对象
                ShrikeBTMethod method = (ShrikeBTMethod)node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    Node left = new Node(node);
                    if(!graph.containsKey(left)){//补充
                                graph.put(left, new HashSet<Node>());
                            }
                            Iterator<CGNode> cgNodeIterator = cg.getPredNodes(node);
                            while(cgNodeIterator.hasNext()){
                                CGNode temp = cgNodeIterator.next();
                                if (temp.getMethod() instanceof ShrikeBTMethod) {
                                    ShrikeBTMethod tmpMethod = (ShrikeBTMethod)node.getMethod();
                                    if("Application".equals(tmpMethod.getDeclaringClass().getClassLoader().toString())){
                                        Node right = new Node(temp);
                                        graph.get(left).add(right);
                                    }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param ChangeInfoPath changeInfo.txt路径
     * @param graph  FindDependency得到的图
     * @param testGraph 测试图
     * @return  result
     */
    public HashSet<String> Select(String ChangeInfoPath, HashMap<Node,HashSet<Node>>graph, HashMap<Node,HashSet<Node>>testGraph){
        HashSet<String>result = new HashSet<String>();
        //读取changeInfo.txt
        ArrayList<String> allChanges = new ArrayList<String>();
        HashSet<String>changedClass = new HashSet<String>();
        HashSet<String>changedMethod = new HashSet<String>();
        try{
            BufferedReader in = new BufferedReader(new FileReader(ChangeInfoPath));
            String str;
            while((str = in.readLine()) != null){
                allChanges.add(str);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        for(int i = 0;i<allChanges.size();i++){
            changedClass.add(allChanges.get(i).split(" ")[0]);
            changedMethod.add(allChanges.get(i).split(" ")[1]);
        }
        //此时记录完了改变的信息
        HashSet<Node>relevantMethodsAndClasses = new HashSet<Node>();
        Queue<Node>NodeList = new LinkedList<Node>();
        for(Node key:graph.keySet()){
            if(condition&&changedClass.contains(key.getClassInnerName())){
                NodeList.add(key);
            }else if(!condition&&changedMethod.contains(key.getSignature())){
                NodeList.add(key);
            }else continue;
        }
        while(!NodeList.isEmpty()) {
            Node node = NodeList.poll();
            if (relevantMethodsAndClasses.contains(node)) {//如果重复了，可能会产生环
                continue;
            }
            relevantMethodsAndClasses.add(node);
            if (graph.containsKey(node)) {
                NodeList.addAll(graph.get(node));
                if (condition == true) {
                    for (Node nodeInGraph : graph.get(node)) {
                        if (testGraph.containsKey(nodeInGraph)) {
                            for (Node testNode : testGraph.keySet()) {
                                if (nodeInGraph.getClassInnerName().equals(testNode.getClassInnerName())
                                        && testNode.cgNode.getMethod().getAnnotations().toString().contains("Test")) {
                                    result.add(testNode.getClassInnerName() + " " + testNode.getSignature());
                                }
                            }
                        }
                    }
                } else {
                    for (Node node1 : graph.get(node)) {
                        if (testGraph.containsKey(node1) && node1.cgNode.getMethod().getAnnotations().toString().contains("Test")) {
                            result.add(node1.getClassInnerName() + " " + node1.getSignature());
                        }
                    }
                }
            }
        }

        return result;
    }

    public void makeCallGraph(){
        try{
            ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
            Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
            cg = new CHACallGraph(cha);
            cg.init(eps);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void AddScope(String path){
        try{
            scope.addClassFileToScope(ClassLoaderReference.Application, new File(path));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void init(boolean C) {
        try{
            scope = AnalysisScopeReader.readJavaScope("scope.txt", new File("exclusion.txt"), ClassLoader.getSystemClassLoader());
            condition = C;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
