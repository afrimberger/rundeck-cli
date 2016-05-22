package org.rundeck.client.tool.commands;

import com.lexicalscope.jewel.cli.CliFactory;
import org.rundeck.client.api.RundeckApi;
import org.rundeck.client.api.model.Execution;
import org.rundeck.client.api.model.JobItem;
import org.rundeck.client.tool.App;
import org.rundeck.client.tool.options.RunBaseOptions;
import org.rundeck.client.util.Client;
import retrofit2.Call;

import java.io.IOException;
import java.util.List;

/**
 * Created by greg on 5/20/16.
 */
public class Run {
    public static void main(final String[] args) throws IOException {
        Client<RundeckApi> client = App.createClient();
        boolean success = run(args, client);
        if (!success) {
            System.exit(2);
        }
    }

    private static boolean run(final String[] args, final Client<RundeckApi> client) throws IOException {
        RunBaseOptions options = CliFactory.parseArguments(RunBaseOptions.class, args);

        String jobId;
        if (options.isJob()) {
            if (!options.isProject()) {
                throw new IllegalArgumentException("-p project is required with -j");
            }
            String job = options.getJob();
            String[] parts = Jobs.splitJobNameParts(job);
            Call<List<JobItem>> listCall = client.getService().listJobs(options.getProject(), parts[1], parts[0]);
            List<JobItem> jobItems = client.checkError(listCall);
            if (jobItems.size() != 1) {
                System.err.printf("Could not find a unique job with name: %s%n", job);
                if (jobItems.size() > 0) {
                    System.err.printf(String.format("Found %d matching jobs:%n", jobItems.size()));
                    for (JobItem jobItem : jobItems) {
                        System.err.printf("* %s%n", jobItem.toBasicString());
                    }
                } else {
                    System.err.println("Found 0 matching jobs.");
                }
                return false;
            }
            JobItem jobItem = jobItems.get(0);
            System.out.printf("Found matching job: %s%n", jobItem.toBasicString());
            jobId = jobItem.getId();
        } else if (options.isId()) {
            jobId = options.getId();
        } else {
            throw new IllegalArgumentException("-j job or -i id is required");

        }
        Call<Execution> executionListCall = client.getService().runJob(
                jobId,
                Adhoc.joinString(options.getCommandString()),
                options.getLoglevel(),
                options.getFilter()
        );
        Execution execution = client.checkError(executionListCall);
        System.out.printf("Execution started: %s%n", execution.toBasicString());

        return Executions.maybeFollow(client, options, execution.getId());
    }
}