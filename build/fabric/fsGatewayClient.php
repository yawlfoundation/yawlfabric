	
<?php
/**
 * Plugin Name: Twitter Demo
 * Plugin URI:  http://example.com/twitter-demo/
 * Description: Retrieves the number of followers and latest Tweet from your Twitter account.
 * Version:     1.0.0
 * Author:      Tom McFarlin
 * Author URI:  http://tommcfarlin.com/
 * License:     GPL-2.0+
 * License URI: http://www.gnu.org/licenses/gpl-2.0.txt
 */
class fsGatewayClient {
    /**
     * Instance of this class.
     *
     * @var      fsGatewayClient
     */
    private static $instance;
 
    /**
     * Initializes the plugin so that the Twitter information is appended to the end of a single post.
     * Note that this constructor relies on the Singleton Pattern
     *
     * @access private
     */
    private function __construct() {
        add_action( 'the_content', array( $this, 'fs gateway communications' ) );
    } // end constructor
 
    /**
     * Creates an instance of this class
     *
     * @access public
     * @return Twitter_Demo    An instance of this class
     */
    public function get_instance() {
        if ( null == self::$instance ) {
            self::$instance = new self;
        }
        return self::$instance;
    } // end get_instance
 
    /**
     * Appends a message to the bottom of a single post including the number of followers and the last Tweet.
     *
     * @access public
     * @param  $content    The post content
     * @return $content    The post content with the Twitter information appended to it.
     */
    public function display_twitter_information( $content ) {
        // If we're on a single post or page...
        if ( is_single() ) {
            // ...attempt to make a response to twitter. Note that you should replace your username here!
            if ( null == ( $json_response = $this--->make_twitter_request('wptuts') ) ) {
 
                // ...display a message that the request failed
                $html = '
<div id="twitter-demo-content">';
 $html .= 'There was a problem communicating with the Twitter API..';
 $html .= '</div>
<!-- /#twitter-demo-content -->';
 
                // ...otherwise, read the information provided by Twitter
            } else {
 
                $html = '
<div id="twitter-demo-content">';
 $html .= 'I have ' . $this->get_follower_count( $json_response ) . ' followers and my last tweet was "' . $this->get_last_tweet( $json_response ) . '".';
 $html .= '</div>
<!-- /#twitter-demo-content -->';
 
            } // end if/else
 
            $content .= $html;
 
        } // end if/else
 
        return $content;
 
    } // end display_twitter_information
 
    /**
     * Attempts to request the specified user's JSON feed from Twitter
     *
     * @access public
     * @param  $username   The username for the JSON feed we're attempting to retrieve
     * @return $json       The user's JSON feed or null of the request failed
     */
    private function addTransactionRequest( $bundle, $amount ) {
 
        $response = wp_remote_get( 'http://localhost:8080/fabricService/fsGateway?action=invoke&bundle=' . $bundle . '&amount=' . $amount );
        try {
 
            // Note that we decode the body's response since it's the actual JSON feed
            $json = json_decode( $response['response'] );
 
        } catch ( Exception $ex ) {
            $json = null;
        } // end try/catch
 
        return $json;
 
    } // end make_twitter_request


    private function queryRequest( $key ) {

        $response = wp_remote_get( 'http://localhost:8080/fabricService/fsGateway?action=query&key=' . $key );
        try {
 
            // Note that we decode the body's response since it's the actual JSON feed
            $json = json_decode( $response['response'] );
 
        } catch ( Exception $ex ) {
            $json = null;
        } // end try/catch
 
        return $json;
 
    } // end make_twitter_request

 
     private function historyRequest( $key ) {

        $response = wp_remote_get( 'http://localhost:8080/fabricService/fsGateway?action=history&key=' . $key );
        try {
 
            // Note that we decode the body's response since it's the actual JSON feed
            $json = json_decode( $response['response'] );
 
        } catch ( Exception $ex ) {
            $json = null;
        } // end try/catch
 
        return $json;
 
    } // end make_twitter_request

 
} // end class
 
// Trigger the plugin
fsGatewayClient::get_instance();