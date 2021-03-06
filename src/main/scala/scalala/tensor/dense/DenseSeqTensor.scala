package scalala;
package tensor;
package dense;

import collection.MergeableSet;
import tensor.Tensor;

final class DenseSeqTensor(val dims: Seq[Int], data: Array[Double])
  extends DoubleArrayData(data) with Tensor[Seq[Int]] with DenseTensor[Seq[Int]] {

  def this(dims: Seq[Int]) = this(dims,new Array(dims.reduceLeft(_ * _)));

  require(dims.reduceLeft(_ * _) == data.size);
  final override def size = data.size;

  @inline final def index(indices: Seq[Int]) : Int = {
    var i = 0;
    var index = 0;
    require(indices.size == dims.size);
    while(i < indices.size) {
      require(indices(i) >= 0 && indices(i) < dims(i));
      index = indices(i) + dims(i) * index;
      i += 1;
    }
    index;
  }

  override def copy = {
    val arr = new Array[Double](size);
    System.arraycopy(data,0,arr,0,size);
    new DenseSeqTensor(dims, arr);
  }

  def domain = new MergeableSet[Seq[Int]] {
    override lazy val size = dims.reduceLeft(_ * _);
    def iterator = {
      def unroll(seq: Seq[Int]) : Iterator[IndexedSeq[Int]]  = {
        if(seq.size == 1) for( i <- 0 until seq(0) iterator) yield IndexedSeq(i);
        else {
          val rest = unroll(seq.tail);
          for { s <- rest; i <- 0 until seq(0) iterator } yield i +: s;
        }
      }
      unroll(dims);
    }

    override def contains(ind: Seq[Int]) = {
      ind.length == dims.length && (0 until ind.length).forall { i => ind(i) == dims(i) }
    }

  }

  def apply(indices: Seq[Int]) = data(index(indices));
  def update(indices: Seq[Int], v: Double) = { data(index(indices)) = v; }

  def like = new DenseSeqTensor(dims);
}
