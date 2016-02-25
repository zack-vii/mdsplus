// package jTraverser;
/** class NodeInfo carries all the NCI information */
import java.io.Serializable;

public class NodeInfo implements Serializable{
    public static final int   CACHED              = 1 << 3;
    public static final int   COMPRESS_ON_PUT     = 1 << 10;
    public static final int   COMPRESS_SEGMENTS   = 1 << 16;
    public static final int   COMPRESSIBLE        = 1 << 8;
    public static final int   DO_NOT_COMPRESS     = 1 << 9;
    public static final int   ESSENTIAL           = 1 << 2;
    public static final int   INCLUDE_IN_PULSE    = 1 << 15;
    public static final int   NID_REFERENCE       = 1 << 14;
    public static final int   NO_WRITE_MODEL      = 1 << 11;
    public static final int   NO_WRITE_SHOT       = 1 << 12;
    public static final int   PARENT_STATE        = 1 << 1;
    public static final int   PATH_REFERENCE      = 1 << 13;
    public static final int   SEGMENTED           = 1 << 5;
    private static final long serialVersionUID    = 4696003180640509579L;
    public static final int   SETUP               = 1 << 6;
    public static final int   STATE               = 1 << 0;
    public static final byte  USAGE_ACTION        = 2;
    public static final byte  USAGE_ANY           = 0;
    public static final byte  USAGE_AXIS          = 10;
    public static final byte  USAGE_COMPOUND_DATA = 12;
    public static final byte  USAGE_DEVICE        = 3;
    public static final byte  USAGE_DISPATCH      = 4;
    public static final byte  USAGE_MAXIMUM       = 12;
    public static final byte  USAGE_NONE          = 1;
    public static final byte  USAGE_NUMERIC       = 5;
    public static final byte  USAGE_SIGNAL        = 6;
    public static final byte  USAGE_STRUCTURE     = 1;
    public static final byte  USAGE_SUBTREE       = 11;
    public static final byte  USAGE_TASK          = 7;
    public static final byte  USAGE_TEXT          = 8;
    public static final byte  USAGE_WINDOW        = 9;
    public static final int   VERSION             = 1 << 4;
    public static final int   WRITE_ONCE          = 1 << 7;

    public static final NodeInfo getNodeInfo(final byte dclass, final byte dtype, final byte usage, final int flags, final int owner, final int length, final int conglomerate_nids, final int conglomerate_elt, final String date_inserted, final String name, final String fullpath, final String minpath, final String path) {
        return new NodeInfo(dclass, dtype, usage, flags, owner, length, conglomerate_nids, conglomerate_elt, date_inserted, name, fullpath, minpath, path);
    }
    private final String date_inserted, name, fullpath, minpath, path;
    private final byte   dtype, dclass, usage;
    private int          flags;
    private final int    owner, length, conglomerate_elt, conglomerate_nids;

    public NodeInfo(final byte dclass, final byte dtype, final byte usage, final int flags, final int owner, final int length, final int conglomerate_nids, final int conglomerate_elt, final String date_inserted, final String name, final String fullpath, final String minpath, final String path){
        this.dclass = dclass;
        this.dtype = dtype;
        this.usage = usage;
        this.flags = flags;
        this.owner = owner;
        this.length = length;
        this.conglomerate_nids = conglomerate_nids;
        this.conglomerate_elt = conglomerate_elt;
        this.date_inserted = date_inserted;
        this.name = name.trim();
        this.fullpath = fullpath;
        this.minpath = minpath;
        this.path = path;
    }

    public final int getConglomerateElt() {
        return this.conglomerate_elt;
    }

    public final int getConglomerateNids() {
        return this.conglomerate_nids;
    }

    public final String getDate() {
        return this.date_inserted;
    }

    public final byte getDClass() {
        return this.dclass;
    }

    public final byte getDType() {
        return this.dtype;
    }

    public final int getFlags() {
        return this.flags;
    }

    public final String getFullPath() {
        return this.fullpath;
    }

    public final int getLength() {
        return this.length;
    }

    public final String getMinPath() {
        return this.minpath;
    }

    public final String getName() {
        return this.name;
    }

    public final int getOwner() {
        return this.owner;
    }

    public final String getPath() {
        return this.path;
    }

    public final byte getUsage() {
        return this.usage;
    }

    public final boolean isCached() {
        return (this.flags & NodeInfo.CACHED) != 0;
    }

    public final boolean isCompressible() {
        return (this.flags & NodeInfo.COMPRESSIBLE) != 0;
    }

    public final boolean isCompressOnPut() {
        return (this.flags & NodeInfo.COMPRESS_ON_PUT) != 0;
    }

    public final boolean isCompressSegments() {
        return (this.flags & NodeInfo.COMPRESS_SEGMENTS) != 0;
    }

    public final boolean isDoNotCompress() {
        return (this.flags & NodeInfo.DO_NOT_COMPRESS) != 0;
    }

    public final boolean isEssential() {
        return (this.flags & NodeInfo.ESSENTIAL) != 0;
    }

    public final boolean isIncludeInPulse() {
        return (this.flags & NodeInfo.INCLUDE_IN_PULSE) != 0;
    }

    public final boolean isNidReference() {
        return (this.flags & NodeInfo.NID_REFERENCE) != 0;
    }

    public final boolean isNoWriteModel() {
        return (this.flags & NodeInfo.NO_WRITE_MODEL) != 0;
    }

    public final boolean isNoWriteShot() {
        return (this.flags & NodeInfo.NO_WRITE_SHOT) != 0;
    }

    public final boolean isOn() {
        return !this.isState();
    }

    public final boolean isParentOn() {
        return !this.isParentState();
    }

    public final boolean isParentState() {
        return (this.flags & NodeInfo.PARENT_STATE) != 0;
    }

    public final boolean isPathReference() {
        return (this.flags & NodeInfo.PATH_REFERENCE) != 0;
    }

    public final boolean isSegmented() {
        return (this.flags & NodeInfo.SEGMENTED) != 0;
    }

    public final boolean isSetup() {
        return (this.flags & NodeInfo.SETUP) != 0;
    }

    public final boolean isState() {
        return (this.flags & NodeInfo.STATE) != 0;
    }

    public final boolean isVersion() {
        return (this.flags & NodeInfo.VERSION) != 0;
    }

    public final boolean isWriteOnce() {
        return (this.flags & NodeInfo.WRITE_ONCE) != 0;
    }

    public final void setFlags(final int flags) {
        this.flags = flags;
    }
}
