package myhadoop.datamining.cluster;

import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.conversion.InputDriver;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.Vector;
import org.apache.mahout.utils.clustering.ClusterDumper;

import com.homework.hdfs.HdfsDAO;

/**
 * 素材我已经改成《Mahout in Action》第七章的Hello world例子，运行正常，结果也书一样
 * @author Administrator
 *书中源码地址： https://github.com/tdunning/MiA
 */
public class KmeansHadoop {
    private static final String HDFS = "hdfs://192.168.0.100:9000";

    public static void main(String[] args) throws Exception {
        //String localFile = "datafile/randomData.csv";
    	String localFile = "datafile/cluster/simple_k-means.txt";
        String inPath = HDFS + "/user/hdfs/mix_data";
        String seqFile = inPath + "/seqfile";
        String seeds = inPath + "/seeds";
        String outPath = inPath + "/result/";
        String clusteredPoints = outPath + "/clusteredPoints";

        JobConf conf = config();
        HdfsDAO hdfs = new HdfsDAO(HDFS, conf);
        hdfs.rmr(inPath);
        hdfs.mkdirs(inPath);
        hdfs.copyFile(localFile, inPath);
        hdfs.ls(inPath);

        InputDriver.runJob(new Path(inPath), new Path(seqFile), "org.apache.mahout.math.RandomAccessSparseVector");

        //int k = 3;
        int k = 2;
        Path seqFilePath = new Path(seqFile);
        Path clustersSeeds = new Path(seeds);
        DistanceMeasure measure = new EuclideanDistanceMeasure();
        clustersSeeds = RandomSeedGenerator.buildRandom(conf, seqFilePath, clustersSeeds, k, measure);
        KMeansDriver.run(conf, seqFilePath, clustersSeeds, new Path(outPath), measure, 0.01, 10, true, 0.01, false);

        Path outGlobPath = new Path(outPath, "clusters-*-final");
        Path clusteredPointsPath = new Path(clusteredPoints);
        System.out.printf("Dumping out clusters from clusters: %s and clusteredPoints: %s\n", outGlobPath, clusteredPointsPath);

        ClusterDumper clusterDumper = new ClusterDumper(outGlobPath, clusteredPointsPath);
        clusterDumper.printClusters(null);
    }

    public static JobConf config() {
        JobConf conf = new JobConf(KmeansHadoop.class);
        conf.setJobName("ItemCFHadoop");
        conf.addResource("classpath:/hadoop/core-site.xml");
        conf.addResource("classpath:/hadoop/hdfs-site.xml");
        conf.addResource("classpath:/hadoop/mapred-site.xml");
        return conf;
    }

    public static void displayCluster(ClusterDumper clusterDumper) {
        Iterator<Integer> keys = clusterDumper.getClusterIdToPoints().keySet().iterator();
        while (keys.hasNext()) {
            Integer center = keys.next();
            System.out.println("Center:" + center);
            for (WeightedVectorWritable point : clusterDumper.getClusterIdToPoints().get(center)) {
                Vector v = point.getVector();
                System.out.println(v.get(0) + "" + v.get(1));
            }
        }
    }
}
