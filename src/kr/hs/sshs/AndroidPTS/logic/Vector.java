package kr.hs.sshs.AndroidPTS.logic;

/**
 * Vectors for points.<br>
 * Has all cool functions vectors have to have.
 * @author stephen
 */
public class Vector {
	private double x;
	private double y;
	private int index;
	
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
		this.index = -1; // Disabled
	}
	public Vector(double x, double y, int index) {
		this.x = x;
		this.y = y;
		this.index = index; // Enabled
	}
	public double x() {
		return this.x;
	}
	public double y() {
		return this.y;
	}
	public void x(double x) {
		this.x = x;
	}
	public void y(double y) {
		this.y = y;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getIndex() {
		return index;
	}
	
	public Vector add(Vector v_add) {
		return new Vector(x + v_add.x(), y + v_add.y());
	}
	public Vector sub(Vector v_sub) {
		return new Vector(x - v_sub.x(), y - v_sub.y());
	}
	public static Vector add(Vector v1, Vector v2){
		return new Vector(v1.x() + v2.x(), v1.y() + v2.y());
	}
	public static Vector sub(Vector v1, Vector v2){
		return new Vector(v1.x() - v2.x(), v1.y() - v2.y());
	}
	public double theta() {
		return Math.atan2(y, x);
	}
	public double length() {
		return Math.sqrt(x*x + y*y);
	}
}
