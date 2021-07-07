package com.systex.sysgateii.gateway.util.test;

import org.junit.Before;
import org.junit.Test;

import com.systex.sysgateii.autosvr.util.DoublyLinkedList.DoublyLinkedList;

import java.util.NoSuchElementException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DoublyLinkedListTest {
	private DoublyLinkedList<Integer> list;

	@Before
	public void setUp() {
		list = new DoublyLinkedList<Integer>();
	}

	@Test
	public void testIsEmptyReturnsTrue() {
		assertTrue(list.isEmpty());
	}

	@Test
	public void testIsEmptySizeIsZero() {
		assertEquals(0, list.size());
	}

	@Test(expected = NoSuchElementException.class)
	public void testRemoveNotPresentThrowsException() {
		list.addFront(1);
		list.remove(2);
	}

	@Test(expected = NoSuchElementException.class)
	public void testAddBeforeNotFoundThrowsException() {
		list.addFront(1);
		list.addBefore(0, 2);
	}

	@Test(expected = NoSuchElementException.class)
	public void testAddAfterNotFoundThrowsException() {
		list.addFront(1);
		list.addAfter(0, 2);
	}

	/**
	 * Output should be: [4,3,2,1,0]
	 */
	@Test
	public void testInsertAtFront() {
		for (int i = 0; i < 5; i++) {
			list.addFront(i);
		}
		assertEquals("[4,3,2,1,0]", list.toString());
	}

	/**
	 * Output should be: [0,1,2,3,4]
	 */
	@Test
	public void testInsertAtEnd() {
		for (int i = 0; i < 5; i++) {
			list.addEnd(i);
		}
		assertEquals("[0,1,2,3,4]", list.toString());
	}

	/**
	 * Output should be: [10,4,3,30,2,1,20,0]
	 */
	@Test
	public void testAddBefore() {
		for (int i = 0; i < 5; i++) {
			list.addFront(i);
		}
		list.addBefore(4, 10);
		list.addBefore(0, 20);
		list.addBefore(2, 30);
		assertEquals("[10,4,3,30,2,1,20,0]", list.toString());
	}

	/**
	 * Output should be: [0,20,1,2,30,3,4,10]
	 */
	@Test
	public void testAddAfter() {
		for (int i = 0; i < 5; i++) {
			list.addEnd(i);
		}
		list.addAfter(4, 10);
		list.addAfter(0, 20);
		list.addAfter(2, 30);
		assertEquals("[0,20,1,2,30,3,4,10]", list.toString());
	}

	/**
	 * Output should be: [10,11,12,13,14]
	 */
	@Test
	public void testRemove() {
		for (int i = 0; i < 15; i++) {
			list.addEnd(i);
		}
		for (int i = 0; i < 10; i++) {
			list.remove(i);
		}
		assertEquals("[10,11,12,13,14]", list.toString());
	}
}