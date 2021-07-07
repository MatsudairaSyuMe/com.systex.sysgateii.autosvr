package com.systex.sysgateii.autosvr.util.DoublyLinkedList;

/**
 * This class represents a node in a Doubly Linked List. The next-variable is a
 * pointer to the next node, and the prev-variable is a pointer to the previous
 * node.
 * <p>
 *
 * @author MatsudairaSyume
 * @see DoublyLinkedList
 */

public class ListNode<AnyType> {
	// The actual data
	AnyType data;
	// Reference to the next node
	ListNode<AnyType> next;
	// Reference to the prev node
	ListNode<AnyType> prev;

	/**
	 * Constructor. Note that the next and prev variables are set to null, thus this
	 * is the "root-node"
	 *
	 * @param data node data
	 */
	ListNode(AnyType data) {
		this(null, data, null);
	}

	/**
	 * Constructor.
	 *
	 * @param data node data
	 * @param next reference to next node
	 * @param prev reference to the previous node
	 */
	ListNode(ListNode<AnyType> prev, AnyType data, ListNode<AnyType> next) {
		this.data = data;
		this.next = next;
		this.prev = prev;
	}
}
