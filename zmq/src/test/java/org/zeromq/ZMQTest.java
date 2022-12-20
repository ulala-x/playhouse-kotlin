package org.zeromq;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;

import static org.junit.jupiter.api.Assertions.*;

public class ZMQTest {
    @Test
    public void testMakeVersion() {
        assertEquals(ZMQ.getFullVersion(),
                ZMQ.makeVersion(ZMQ.getMajorVersion(), ZMQ.getMinorVersion(), ZMQ.getPatchVersion()));

        System.out.println(ZMQ.getFullVersion());
    }

    /**
     * Test method for {@link ZMQ#getVersionString()}.
     */
    @Test
    public void testGetVersion() {
        assertEquals(ZMQ.getMajorVersion() + "." + ZMQ.getMinorVersion() + "." + ZMQ.getPatchVersion(),
                ZMQ.getVersionString());
    }

    /**
     * Test method for {@link ZMQ.Socket#bindToRandomPort(String)}.
     */
    @Test
    public void testBindToRandomPort() {
//        ZMQ.Context context = ZMQ.context(1);
//        ZMQ.Socket sock = context.socket(ZMQ.DEALER);
//
//        // Check that bindToRandomport generate valid port number
//        for (int i = 0; i < 100; i++) {
//            sock.bindToRandomPort("tcp://127.0.0.1");
//        }
//
//        sock.close();
//        sock = context.socket(ZMQ.DEALER);
//
//        // Check that exception different of EADDRINUSE is not catched
//
//        // Invalid protocol
//        try {
//            sock.bindToRandomPort("noprotocol://127.0.0.1");
//        } catch (ZMQException e) {
//            assertEquals(e.getErrorCode(), ZMQ.EPROTONOSUPPORT());
//        }
    }

    /**
     * Test method for {@link ZMQ.Socket#bindToSystemRandomPort(String)}.
     */
    @Test
    public void testBindToSystemRandomPort() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket sock = context.socket(ZMQ.DEALER);

        sock.bindToSystemRandomPort("tcp://127.0.0.1");
        sock.close();
    }

    @Test
    public void testReqRep() {
        ZMQ.Context context = ZMQ.context(1);

        ZMQ.Socket in = context.socket(ZMQ.REQ);
        in.bind("inproc://reqrep");

        ZMQ.Socket out = context.socket(ZMQ.REP);
        out.connect("inproc://reqrep");

        for (int i = 0; i < 10; i++) {
            byte[] req = ("request" + i).getBytes();
            byte[] rep = ("reply" + i).getBytes();

            assertTrue(in.send(req, 0));
            byte[] reqTmp = out.recv(0);
            assertArrayEquals(req, reqTmp);

            assertTrue(out.send(rep, 0));
            byte[] repTmp = in.recv(0);
            assertArrayEquals(rep, repTmp);
        }
    }
    /**
     * Test method for various set/get options.
     */
    @Test
    public void testSetOption() {
        ZMQ.Context context = ZMQ.context(1);

        ZMQ.Socket sock = context.socket(ZMQ.REQ);

        if (ZMQ.getFullVersion() >= ZMQ.makeVersion(3, 2, 0)) {
            sock.setIPv4Only(false);
            assertEquals(false, sock.getIPv4Only());

            sock.setIPv4Only(true);
            assertEquals(true, sock.getIPv4Only());
        }
        sock.close();

        context.term();
    }

    static class Client extends Thread {

        private ZMQ.Socket s = null;
        private String name = null;

        public Client(ZMQ.Context ctx, String name_) {
            s = ctx.socket(ZMQ.REQ);
            name = name_;

            s.setIdentity(name.getBytes());
        }

        @Override
        public void run() {
            s.connect("tcp://127.0.0.1:6660");
            s.send("hello", 0);
            String msg = s.recvStr(0);
            s.send("world", 0);
            msg = s.recvStr(0);

            s.close();
        }
    }

    static class Dealer extends Thread {

        private ZMQ.Socket s = null;
        private String name = null;

        public Dealer(ZMQ.Context ctx, String name_) {
            s = ctx.socket(ZMQ.DEALER);
            name = name_;

            s.setIdentity(name.getBytes());
        }

        @Override
        public void run() {

            s.connect("tcp://127.0.0.1:6661");
            int count = 0;
            while (count < 2) {
                String msg = s.recvStr(0);
                if (msg == null) {
                    throw new RuntimeException();
                }
                String identity = msg;
                msg = s.recvStr(0);
                if (msg == null) {
                    throw new RuntimeException();
                }

                msg = s.recvStr(0);
                if (msg == null) {
                    throw new RuntimeException();
                }

                s.send(identity, ZMQ.SNDMORE);
                s.send("", ZMQ.SNDMORE);
                String response = "OK " + msg;

                s.send(response, 0);
                count++;
            }
            s.close();
        }
    }

    static class Main extends Thread {

        ZMQ.Context ctx;

        Main(ZMQ.Context ctx_) {
            ctx = ctx_;
        }

        @Override
        public void run() {
            ZMQ.Socket frontend = ctx.socket(ZMQ.ROUTER);

            assertNotNull(frontend);
            frontend.bind("tcp://127.0.0.1:6660");

            ZMQ.Socket backend = ctx.socket(ZMQ.DEALER);
            assertNotNull(backend);
            backend.bind("tcp://127.0.0.1:6661");

            ZMQ.proxy(frontend, backend, null);

            frontend.close();
            backend.close();
        }

    }

    @Test
    public void testProxy() throws Exception {

        if (ZMQ.getFullVersion() < ZMQ.make_version(3, 2, 2)) {
            // Can only test zmq_proxy on ZMQ >= of 3.2.2
            return;
        }

        ZMQ.Context ctx = ZMQ.context(1);
        assert (ctx != null);

        Main mt = new Main(ctx);
        mt.start();
        new Dealer(ctx, "AA").start();
        new Dealer(ctx, "BB").start();

        Thread.sleep(1000);
        Thread c1 = new Client(ctx, "X");
        c1.start();

        Thread c2 = new Client(ctx, "Y");
        c2.start();

        c1.join();
        c2.join();

        ctx.term();
    }

    /**
     * Test method for Router Mandatory
     */
    @Test
    public void testRouterMandatory() {
        if (ZMQ.getFullVersion() < ZMQ.makeVersion(3, 2, 0))
            return;

        ZMQ.Context context = ZMQ.context(1);

        ZMQ.Socket sock = context.socket(ZMQ.ROUTER);
        boolean ret = sock.sendMore("UNREACHABLE");
        assertEquals(true, ret);
        sock.send("END");

        sock.setRouterMandatory(true);
        try {
            sock.sendMore("UNREACHABLE");
            assertFalse(true);
        } catch (ZMQException e) {
            assertEquals(ZMQ.EHOSTUNREACH(), e.getErrorCode());
        }

        sock.close();
        context.term();
    }

    @Test
    public void testSendMoreRequestReplyOverTcp() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket reply = null;
        ZMQ.Socket socket = null;
        try {
            reply = context.socket(ZMQ.REP);
            reply.bind("tcp://*:12345");
            socket = context.socket(ZMQ.REQ);
            socket.connect("tcp://localhost:12345");
            socket.send("test1", ZMQ.SNDMORE);
            socket.send("test2");
            assertEquals("test1", reply.recvStr());
            assertTrue(reply.hasReceiveMore());
            assertEquals("test2", reply.recvStr());
        } finally {
            try {
                socket.close();
            } catch (Exception ignore){}
            try {
                reply.close();
            } catch (Exception ignore){}
            try {
                context.term();
            } catch (Exception ignore) {}
        }
    }


    @Test
    public void testZeroCopyRecv() {
        if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0)) {
            ZMQ.Context context = ZMQ.context(1);

            ByteBuffer response = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder());
            ZMQ.Socket push = null;
            ZMQ.Socket pull = null;
            try {
                push = context.socket(ZMQ.PUSH);
                pull = context.socket(ZMQ.PULL);
                pull.bind("tcp://*:45324");
                push.connect("tcp://localhost:45324");

                push.send("PING");
                int rc = pull.recvZeroCopy(response, 16, 0);
                response.flip();
                byte[] b = new byte[rc];
                response.get(b);
                assertEquals("PING", new String(b));
            } finally {
                try {
                    push.close();
                } catch (Exception ignore) {
                }
                try {
                    pull.close();
                } catch (Exception ignore) {
                }
                try {
                    context.term();
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Test
    public void testZeroCopySend() throws InterruptedException {
        if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0)) {
            ZMQ.Context context = ZMQ.context(1);
            ByteBuffer bb = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder());
            ZMQ.Socket push = null;
            ZMQ.Socket pull = null;
            try {
                push = context.socket(ZMQ.PUSH);
                pull = context.socket(ZMQ.PULL);
                pull.bind("tcp://*:45324");
                push.connect("tcp://localhost:45324");
                bb.put("PING".getBytes());
                push.sendZeroCopy(bb, bb.position(), 0);
                assertEquals("PING", new String(pull.recv()));
            } finally {
                try {
                    push.close();
                } catch (Exception ignore) {
                }
                try {
                    pull.close();
                } catch (Exception ignore) {
                }
                try {
                    context.term();
                } catch (Exception ignore) {
                }
            }
        }
    }



    @Test
    public void testByteBufferRecvTooLarge() throws InterruptedException, CharacterCodingException {
        if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0)) {
            ZMQ.Context context = ZMQ.context(1);
            ByteBuffer bb = ByteBuffer.allocateDirect(5).order(ByteOrder.nativeOrder());
            ZMQ.Socket push = null;
            ZMQ.Socket pull = null;
            try {
                push = context.socket(ZMQ.PUSH);
                pull = context.socket(ZMQ.PULL);
                pull.bind("tcp://*:6787");
                push.connect("tcp://127.0.0.1:6787");
                push.send("helloworld".getBytes(), 0);
                int size = pull.recvByteBuffer(bb, 0);
                bb.flip();
                byte[] b = new byte[size];
                bb.get(b);
                assertEquals("hello", new String(b));
            } finally {
                try {
                    push.close();
                } catch (Exception ignore) {
                }
                try {
                    pull.close();
                } catch (Exception ignore) {
                }
                try {
                    context.term();
                } catch (Exception ignore) {
                }
            }
        }
    }
    @Test
    public void testPollerUnregister() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socketOne = context.socket(ZMQ.SUB);
        ZMQ.Socket socketTwo = context.socket(ZMQ.REP);
        ZMQ.Poller poller = new ZMQ.Poller(2);
        poller.register(socketOne, ZMQ.Poller.POLLIN);
        poller.register(socketTwo, ZMQ.Poller.POLLIN);

        socketOne.setLinger(0);
        socketOne.close();
        socketTwo.setLinger(0);
        socketTwo.close();

        poller.unregister(socketOne);
        poller.unregister(socketTwo);

        context.term();
    }



    @Test
    public void testEventConnected() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket helper = context.socket(ZMQ.REQ);
        int port = helper.bindToRandomPort("tcp://127.0.0.1");

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        monitor.setReceiveTimeOut(100);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_CONNECTED));
        monitor.connect("inproc://monitor.socket");

        socket.connect("tcp://127.0.0.1:" + port);
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_CONNECTED, event.getEvent());

        helper.close();
        socket.close();
        monitor.close();
        context.term();
    }

    @Test
    public void testEventConnectDelayed() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        monitor.setReceiveTimeOut(100);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_CONNECT_DELAYED));
        monitor.connect("inproc://monitor.socket");

        socket.connect("tcp://127.0.0.1:6751");
        event = ZMQ.Event.recv(monitor);
        assertNotNull( event,"No event was received");
        assertEquals(ZMQ.EVENT_CONNECT_DELAYED, event.getEvent());

        socket.close();
        monitor.close();
        context.term();
    }


    @Test
    public void testEventListening() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        monitor.setReceiveTimeOut(100);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_LISTENING));
        monitor.connect("inproc://monitor.socket");

        socket.bindToRandomPort("tcp://127.0.0.1");
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_LISTENING, event.getEvent());

        socket.close();
        monitor.close();
        context.term();
    }

    @Test
    public void testEventBindFailed() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket helper = context.socket(ZMQ.REP);
        int port = helper.bindToRandomPort("tcp://127.0.0.1");

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        monitor.setReceiveTimeOut(100);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_BIND_FAILED));
        monitor.connect("inproc://monitor.socket");

        try {
            socket.bind("tcp://127.0.0.1:" + port);
        } catch (ZMQException ex) {}
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_BIND_FAILED, event.getEvent());

        helper.close();
        socket.close();
        monitor.close();
        context.term();
    }

    @Test
    public void testEventAccepted() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        ZMQ.Socket helper = context.socket(ZMQ.REQ);
        monitor.setReceiveTimeOut(100);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_ACCEPTED));
        monitor.connect("inproc://monitor.socket");

        int port = socket.bindToRandomPort("tcp://127.0.0.1");

        helper.connect("tcp://127.0.0.1:" + port);
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_ACCEPTED, event.getEvent());

        helper.close();
        socket.close();
        monitor.close();
        context.term();
    }

    @Test
    public void testEventClosed() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        monitor.setReceiveTimeOut(100);

        socket.bindToRandomPort("tcp://127.0.0.1");

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_CLOSED));
        monitor.connect("inproc://monitor.socket");

        socket.close();
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_CLOSED, event.getEvent());

        monitor.close();
        context.term();
    }

    @Test
    public void testEventDisconnected() {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2)) // Monitor added in 3.2.2
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        ZMQ.Socket helper = context.socket(ZMQ.REQ);
        monitor.setReceiveTimeOut(100);

        int port = socket.bindToRandomPort("tcp://127.0.0.1");
        helper.connect("tcp://127.0.0.1:" + port);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_DISCONNECTED));
        monitor.connect("inproc://monitor.socket");

        byte[] req ="request".getBytes();
        helper.send(req, 100);
        assertArrayEquals(socket.recv(100), req);

        helper.close();
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_DISCONNECTED, event.getEvent());

        socket.close();
        monitor.close();
        context.term();
    }

    @Test
    public void testEventMonitorStopped() {
        if (ZMQ.version_full() < ZMQ.make_version(4, 0, 0)) // EVENT_MONITOR_STOPPED added in 4.0.0
            return;

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Event event;

        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket monitor = context.socket(ZMQ.PAIR);
        monitor.setReceiveTimeOut(100);

        assertTrue(socket.monitor("inproc://monitor.socket", ZMQ.EVENT_MONITOR_STOPPED));
        monitor.connect("inproc://monitor.socket");

        socket.monitor(null, 0);
        event = ZMQ.Event.recv(monitor);
        assertNotNull(event,"No event was received");
        assertEquals(ZMQ.EVENT_MONITOR_STOPPED, event.getEvent());

        socket.close();
        monitor.close();
        context.term();
    }



}
