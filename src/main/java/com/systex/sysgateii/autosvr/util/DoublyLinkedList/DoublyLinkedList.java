package com.systex.sysgateii.autosvr.util.DoublyLinkedList;

import java.util.NoSuchElementException;

public class DoublyLinkedList<AnyType> {
	// Front / head node
	private ListNode<AnyType> front;
	// Helper, keeping track of size.
	private int size;

	/**
	 * Constructing empty list.
	 */
	public DoublyLinkedList() {
		front = null;
	}

	/**
	 * Adding a node to the front of the list.
	 *
	 * @param x Value to add
	 */
	public void addFront(AnyType x) {
		if (isEmpty())
			front = new ListNode<AnyType>(x);
		else {
			ListNode<AnyType> temp = front;
			front = new ListNode<AnyType>(null, x, temp);
			front.next.prev = front;
		}
		size++;
	}

	/**
	 * Inserting a node at the end of the list.
	 *
	 * @param x Value to add.
	 */
	public void addEnd(AnyType x) {
		if (isEmpty())
			front = new ListNode<AnyType>(x);
		else {
			ListNode<AnyType> temp = front;
			// Traverse till end of list
			while (temp.next != null) {
				temp = temp.next;
			}
			temp.next = new ListNode<AnyType>(temp, x, null);
		}
		size++;
	}

	/**
	 * get node before another node
	 *
	 * @param get the node before the x Value, if found
	 *
	 */
	public AnyType getPrev(AnyType x) {
		// List is empty, can't add
		if (isEmpty())
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		ListNode<AnyType> current = front;
		// Looping through until found
		while (current != null && !current.data.equals(x))
			current = current.next;
		// If null, not found
		if (current == null)
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		ListNode<AnyType> prevNode = current.prev;
		if (prevNode == null)
			throw new NoSuchElementException("Element " + x.toString() + " is the head node");
		return prevNode.data;
	}

	/**
	 * get node after another node
	 *
	 * @param get the node after the x Value, if found
	 *
	 */
	public AnyType getAfter(AnyType x) {
		if (isEmpty())
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		ListNode<AnyType> current = front;
		// Looping through until found
		while (current != null && !current.data.equals(x))
			current = current.next;
		// If null, not found
		if (current == null)
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		// Not null, value found
		ListNode<AnyType> afterNode = current.next;
		if (afterNode == null)
			throw new NoSuchElementException("Element " + x.toString() + " is the end node");
		return afterNode.data;
	}

	/**
	 * Adding node before another node
	 *
	 * @param x Value to look for, adding before x if found
	 * @param y Value to add.
	 */
	public void addBefore(AnyType x, AnyType y) {
		// List is empty, can't add
		if (isEmpty())
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		ListNode<AnyType> current = front;
		// Looping through until found
		while (current != null && !current.data.equals(x))
			current = current.next;
		// If null, not found
		if (current == null)
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		ListNode<AnyType> newNode = new ListNode<AnyType>(current.prev, y, current);
		if (current.prev != null)
			current.prev.next = newNode;
		else
			front = newNode;
		current.prev = newNode;
		size++;
	}

	/**
	 * Adding node after another node
	 *
	 * @param x Value to look for, adding after x if found
	 * @param y Value to add.
	 */
	public void addAfter(AnyType x, AnyType y) {
		if (isEmpty())
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		ListNode<AnyType> current = front;
		// Looping through until found
		while (current != null && !current.data.equals(x))
			current = current.next;
		// If null, not found
		if (current == null)
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		// Not null, value found
		ListNode<AnyType> newNode = new ListNode<AnyType>(current, y, current.next);
		if (current.next != null)
			current.next.prev = newNode;
		current.next = newNode;
		size++;
	}

	/**
	 * Removing a Node from the list.
	 *
	 * @param x Value to remove
	 */
	public void remove(AnyType x) {
		if (isEmpty())
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		// Removing front element
		if (front.data.equals(x)) {
			front = front.next;
			return;
		}
		ListNode<AnyType> current = front;
		// Looping through until found
		while (current != null && !current.data.equals(x))
			current = current.next;
		// If null, not found
		if (current == null)
			throw new NoSuchElementException("Element " + x.toString() + " not found");
		// It has a next pointer, so not the last node.
		if (current.next != null)
			current.next.prev = current.prev;
		current.prev.next = current.next;
		size--;
	}

	/**
	 * @return true if list is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * @return size of list
	 */
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		ListNode<AnyType> temp = front;
		StringBuilder builder = new StringBuilder("[");
		while (temp != null) {
			builder.append(temp.data).append(",");
			temp = temp.next;
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("]");
		return builder.toString();
	}
}
