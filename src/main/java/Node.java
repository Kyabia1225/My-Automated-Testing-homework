import com.ibm.wala.ipa.callgraph.CGNode;

public class Node {
    CGNode cgNode;
    public Node(CGNode cgn){
        cgNode = cgn;
    }

    public String getClassInnerName(){
        return cgNode.getMethod().getDeclaringClass().getName().toString();
    }
    public String getSignature(){
        return cgNode.getMethod().getSignature();
    }

    public boolean equals(Object obj) {//override
        return (getClassInnerName()+" "+getSignature()).equals(((Node) obj).getClassInnerName()+" "+((Node) obj).getSignature());
    }
    public int hashCode(){//override
        return (getClassInnerName()+" "+getSignature()).hashCode();
    }
}
