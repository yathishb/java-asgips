package asgips;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import org.apache.log4j.PropertyConfigurator;

public class ASGIPs {
	public static void main(String[] args) {
        List<String> instanceIPs = new ArrayList<String>();
        instanceIPs = getASGIPs();
        System.out.println(instanceIPs);
    }

    public static List<String> getASGIPs() {
        Properties prop = new Properties();
        prop.setProperty("log4j.rootLogger", "WARN");
        PropertyConfigurator.configure(prop);
        String groupName = "test1234";
        Region region = Region.AP_SOUTH_1;
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();
        AutoScalingClient autoScalingClient = AutoScalingClient.builder()
                .region(region)
                .build();
        List<String> instanceIPs = new ArrayList<String>();
        instanceIPs = getAutoScaling(ec2, autoScalingClient, groupName);
        autoScalingClient.close();
        ec2.close();
        return instanceIPs;
    }

    public static String describeEC2Instances(Ec2Client ec2, String instanceid) {
        try {
            String instanceprivateip = "";
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instanceid)
                    .build();
            DescribeInstancesResponse response = ec2.describeInstances(request);
            for (Reservation reservation : response.reservations()) {
                for (software.amazon.awssdk.services.ec2.model.Instance instance : reservation.instances()) {
                    instanceprivateip = instance.privateIpAddress();
                }
            }
            return instanceprivateip;

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorCode());
            System.exit(1);
        }
        return "";
    }

    public static List<String> getAutoScaling(Ec2Client ec2, AutoScalingClient autoScalingClient, String groupName) {
        List<String> instanceIPs = new ArrayList<String>();
        try {
            DescribeAutoScalingGroupsRequest scalingGroupsRequest = DescribeAutoScalingGroupsRequest.builder()
                    .autoScalingGroupNames(groupName)
                    .build();

            DescribeAutoScalingGroupsResponse response = autoScalingClient
                    .describeAutoScalingGroups(scalingGroupsRequest);
            List<AutoScalingGroup> groups = response.autoScalingGroups();
            for (AutoScalingGroup group : groups) {
                List<Instance> instances = group.instances();
                System.out.println("groups" + group);
                for (Instance instance : instances) {
                    instanceIPs.add(describeEC2Instances(ec2, instance.instanceId()));
                }
            }
            return instanceIPs;
        } catch (AutoScalingException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return instanceIPs;
    }
}
