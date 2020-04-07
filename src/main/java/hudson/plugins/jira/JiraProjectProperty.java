package hudson.plugins.jira;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.Util;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

/**
 * Associates {@link Job} with {@link JiraSite}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JiraProjectProperty extends JobProperty<Job<?, ?>> {

    /**
     * Used to find {@link JiraSite}. Matches {@link JiraSite#getName()}. Always
     * non-null (but beware that this value might become stale if the system
     * config is changed.)
     */
    public final String siteName;

    @DataBoundConstructor
    public JiraProjectProperty(String siteName) {
        siteName = Util.fixEmptyAndTrim(siteName);
        if (siteName == null) {
            // defaults to the first one
            List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
            if (!sites.isEmpty()) {
                siteName = sites.get(0).getName();
            }
        }
        this.siteName = siteName;
    }

    /**
     * Gets the {@link JiraSite} that this project belongs to.
     *
     * @return null if the configuration becomes out of sync.
     */
    @Nullable
    public JiraSite getSite() {
        List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();

        if (siteName == null && sites.size() > 0) {
            // default
            return sites.get(0);
        }

        Stream<JiraSite> streams = sites.stream();
        if (owner != null) {
            Stream<JiraSite> stream2 = JiraFolderProperty.getSitesFromFolders(owner.getParent())
                .stream();
            streams = Stream.concat(streams, stream2).parallel();
        }

        return streams.filter(jiraSite -> jiraSite.getName().equals(siteName))
            .findFirst().orElse(null);
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(Class<? extends Job> jobType) {
            return Job.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraProjectProperty_DisplayName();
        }

        /**
         * @deprecated use {@link JiraGlobalConfiguration#setSites(List)} instead
         *
         * @param site the Jira site
         */
        @Deprecated
        public void setSites(JiraSite site) {
            JiraGlobalConfiguration.get().getSites().add(site);
        }

        /**
         * @deprecated use {@link JiraGlobalConfiguration#getSites()} instead
         *
         * @return array of sites
         */
        @Deprecated
        public JiraSite[] getSites() {
            return JiraGlobalConfiguration.get().getSites().toArray(new JiraSite[0]);
        }

        @SuppressWarnings("unused") // Used by stapler
        public ListBoxModel doFillSiteNameItems(@AncestorInPath AbstractFolder<?> folder) {
            ListBoxModel items = new ListBoxModel();
            for (JiraSite site : JiraGlobalConfiguration.get().getSites()) {
                items.add(site.getName());
            }
            if (folder != null) {
                List<JiraSite> sitesFromFolder = JiraFolderProperty.getSitesFromFolders(folder);
                sitesFromFolder.stream().map(JiraSite::getName).forEach(items::add);
            }
            return items;
        }

    }
}
