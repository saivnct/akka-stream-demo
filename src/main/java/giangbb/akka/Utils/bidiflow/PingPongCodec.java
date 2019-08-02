package giangbb.akka.Utils.bidiflow;

import akka.util.ByteIterator;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;

import java.nio.ByteOrder;

public class PingPongCodec {
    public static ByteString toBytes(Message message){
        if (message instanceof Ping){
            final int id = ((Ping) message).id;
            return new ByteStringBuilder().putByte((byte)1).putInt(id,ByteOrder.LITTLE_ENDIAN).result();
        }else if (message instanceof Pong){
            final int id = ((Pong) message).id;
            return new ByteStringBuilder().putByte((byte)2).putInt(id,ByteOrder.LITTLE_ENDIAN).result();
        }
        return null;
    }

    public static Message fromBytes(ByteString byteString){
        final ByteIterator it = byteString.iterator();
        switch (it.getByte()){
            case 1:
                return new Ping(it.getInt(ByteOrder.LITTLE_ENDIAN));
            case 2:
                return new Pong(it.getInt(ByteOrder.LITTLE_ENDIAN));
        }
        return null;
    }

    public static ByteString addLengthHeader(ByteString bytes) {
        final int len = bytes.size();
        return new ByteStringBuilder().putInt(len, ByteOrder.LITTLE_ENDIAN).append(bytes).result();
    }

}

