package application.test.cmd;

import java.io.IOException;
import java.util.Arrays;

import com.beust.jcommander.JCommander;

public class JCommanderTest {
	
	public static final String[] dev_args = new String[]{
			"-machine=mac",
			"-ucr=ItalyPowerDemand", // ToeSegmentation2 ItalyPowerDemand
			"-out=output/dev/tmp/it",
			"-trees=100",
			
//			"-s=ee:5,boss:100,rif:100",
//			"-s=ee:5,boss:100,rif:100,st:100",
//			"-s=ee:5,boss:100,rif:100,st:100",
//			"-s=ee:5,boss:100,rif:100,it:100",			
			"-s=it:1",
//			"-s=ee:5,rif:10,st:10",
			
			"-it_m=100",
			
//			"-st_min_length_percent=0",
//			"-st_max_length_percent=1",
			"-st_min_length=10",
			"-st_max_length=0",
			"-st_interval_method=lengthfirst", //{lengthfirst, swap} method used to sample intervals
//			"-s=st:100",
			"-st_params=preload_data",  //{random, preload,preload_data} --randomly or load params from files (default location for -st_params_files=settings/st)
			"-st_threshold_selection=bestgini", //how do we choose split point ? { bestgini, median, random}
			"-st_num_rand_thresholds=1", // > 1
			"-st_feature_selection=int",
			"-st_params_files=settings/st-5356/",

//			"-ee_dm=euc",
//			"-ee_dm=euc,dtw,dtwr,ddtw,ddtwr,wdtw,wddtw,lcss,twe,erp,msm", //
			
			"-threads=0",
			"-repeats=1",
			"-export=3",
			"-results=output/dev/tmp/_results.txt",
			//-- SEED doesnt work for BOSS due to HPPC library, it also doesnt work when -threads > 1
			"-seed=0",
			"--root=../slurm",
			"--wdir=../pyts"
	};	
	
	public static void main(String[] cmd_args) throws IOException, IllegalArgumentException, IllegalAccessException {

		cmd_args = dev_args;
		
		CMDArgs args  =  new CMDArgs();
		JCommander.newBuilder()
		  .addObject(args)
		  .build()
		  .parse(cmd_args);
		
		System.out.println(Arrays.toString(args.generate_kernerls));
		System.out.println(Arrays.toString(args.parameters.toArray(new String[args.parameters.size()])));
		System.out.println(args.dynamicParams.toString());		
		System.out.println(Arrays.toString(args.toArray()));	
		
	}

}
