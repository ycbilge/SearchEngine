
public class IndexTuple {
	private int left;
	private int right;
	public IndexTuple(int l, int r) {
		setLeft(l);
		setRight(r);
	}
	public int getLeft() {
		return left;
	}
	public void setLeft(int left) {
		this.left = left;
	}
	public int getRight() {
		return right;
	}
	public void setRight(int right) {
		this.right = right;
	}
	@Override
	public boolean equals(Object i){ 
	    if (!(i instanceof IndexTuple)) {
	        return false;
	    }
	    IndexTuple in = (IndexTuple) i;
	    if(this.left == in.left && this.right == in.right) {
	    	return true;
	    }else {
	    	return false;
	    }
	    //return this.size.equals(dog.size);
	}
}
 