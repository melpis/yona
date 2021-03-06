/**
 * @author Ahn Hyeok Jun
 */

package models;

import static com.avaje.ebean.Expr.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import models.enumeration.ResourceType;
import models.resource.Resource;
import utils.JodaDateUtil;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "number"}))
public class Posting extends AbstractPosting {
    private static final long serialVersionUID = 5287703642071155249L;

    public static final Finder<Long, Posting> finder = new Finder<>(Long.class, Posting.class);

    public boolean notice;
    public boolean readme;

    @Transient
    public String issueTemplate;

    //ToDo: Sperate it from posting for online commit
    @Transient
    public String path;

    //ToDo: Sperate it from posting for online commit
    @Transient
    public String branch;

    //ToDo: Sperate it from posting for online commit
    @Transient
    public String lineEnding;

    @OneToMany(cascade = CascadeType.ALL)
    public List<PostingComment> comments;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    public Set<IssueLabel> labels;

    public Set<Long> getLabelIds() {
        Set<Long> labelIds = new HashSet<>();

        for(IssueLabel label : this.labels){
            labelIds.add(label.id);
        }

        return labelIds;
    }

    public Posting(Project project, User author, String title, String body) {
        super(project, author, title, body);
    }

    /**
     * @see models.Project#increaseLastPostingNumber()
     */
    @Override
    protected Long increaseNumber() {
        return Project.increaseLastPostingNumber(project.id);
    }

    protected void fixLastNumber() {
        Project.fixLastPostingNumber(project.id);
    }

    /**
     * @see models.AbstractPosting#computeNumOfComments()
     */
    public int computeNumOfComments() {
        return comments.size();
    }

    @OneToOne
    public Posting parent;

    public Posting() {
        super();
    }

    @Override
    public Resource asResource() {
        return asResource(ResourceType.BOARD_POST);
    }

    public static List<Posting> findNotices(Project project) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .add(eq("notice", true))
                .order().desc("createdDate")
                .findList();
    }

    public static List<Posting> findRecentlyCreated(Project project, int size) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .order().desc("createdDate")
                .findPagingList(size).getPage(0)
                .getList();
    }

    public static List<Posting> findRecentlyCreatedByDaysAgo(Project project, int days) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .ge("createdDate", JodaDateUtil.before(days)).order().desc("createdDate").findList();
    }

    /**
     * @see models.AbstractPosting#getComments()
     */
    @Transient
    public List<? extends Comment> getComments() {
        Collections.sort(comments, Comment.comparator());
        return comments;
    }

    @Override
    public void checkLabels() {

    }

    public static Posting findByNumber(Project project, long number) {
        return AbstractPosting.findByNumber(finder, project, number);
    }

    public static int countPostings(Project project) {
        return finder.where().eq("project", project).findRowCount();
    }

    /**
     * use EBean save functionality directly
     * to prevent occurring select table lock
     */
    public void directSave(){
        super.directSave();
    }

    public static Posting findREADMEPosting(Project project) {
        return Posting.finder.where()
                .eq("project.id", project.id)
                .add(eq("readme", true))
                .findUnique();
    }

    public PostingComment findCommentByCommentId(Long id) {
        for (PostingComment comment: comments) {
            if (comment.id.equals(id)) {
                return comment;
            }
        }
        return null;
    }

    public static Posting from(Issue issue) {
        Posting posting = new Posting();

        posting.title = issue.title;
        posting.body = issue.body;
        posting.history = issue.history;
        posting.createdDate = issue.createdDate;
        posting.updatedDate = issue.updatedDate;
        posting.authorId = issue.authorId;
        posting.authorLoginId = issue.authorLoginId;
        posting.authorName = issue.authorName;
        posting.project = issue.project;

        return posting;
    }
}
