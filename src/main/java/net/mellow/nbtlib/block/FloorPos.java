package net.mellow.nbtlib.block;

public class FloorPos implements Comparable<FloorPos> {

    /**
     * Pretty much identical to BlockPos, but will provide the same hash in the same column
     */

    public int x;
    public int z;

    public FloorPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + z;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FloorPos other = (FloorPos) obj;
        if (x != other.x)
            return false;
        if (z != other.z)
            return false;
        return true;
    }

    @Override
    public int compareTo(FloorPos o) {
        return equals(o) ? 0 : 1;
    }

}
