package com.systex.sysgateii.autosvr.util;
//20220819 MatsudairaSyuMe
public enum COMM_STATE {
	TRANSF((byte) 0x0), CANCEL((byte) (0x1 & 0xff)), CHECK((byte) (0x2 & 0xff)), ACK((byte) (0x3 & 0xff)),
	NAK((byte) (0x4 & 0xff));

	private byte id;

	COMM_STATE(final byte value) {
		this.id = value;
	}

	public byte Getid() {
		return id;
	}

	public static COMM_STATE ById(final byte num) {
		for (COMM_STATE e : COMM_STATE.values()) {
			if (e.id == num) {
				return e;
			}
		}
		return null;
	}
}
